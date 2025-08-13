// src/main/java/com/example/memo/chatbot/service/EmbeddingService.java
package com.example.memo.chatbot.service;

import com.example.memo.jpa.entity.chatbot.EmbeddingChunk;
import com.example.memo.jpa.repository.chatbot.EmbeddingChunkRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.postgresql.util.PGobject;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingChunkRepository embeddingChunkRepository;

    // ✅ 권장 경로: EmbeddingModel + JdbcTemplate로 직접 배치 insert (VectorStore 새로 정의하지 않음)
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate pgJdbcTemplate; // PostgreSQL DataSource에 연결된 JdbcTemplate

    //PDF 텍스트 추출
    private final FileService fileService;
    //PDF 리소스 스캔
    private final ResourcePatternResolver resourcePatternResolver;
    //한 청크 최대 길이
    private static final int CHUNK_SIZE = 1500;
    //앞/뒤 청크 겹치는 길이(청크1: [0~1499] 청크2: [1450~2949] ← 앞부분 50자(1450~1499)가 청크1과 겹침
    //문맥 유지,검색 정확도 향상 가능
    private static final int CHUNK_OVERLAP = 50;

    // 성능 파라미터
    //임베딩/저장 배치 크기
    private static final int BATCH_ADD_SIZE = 64;
    //세마포어:놀이동산에 입장할 때 한 번에 8명까지만 들어가고 다음 들어가려면 사람이 나오고 들어갈 수 있도록 제한
    //동시에 PDF 임베딩 API 호출을 수행할 수 있는 작업 최대 개수를 8로 제한
    private static final int MAX_EMBED_CONCURRENCY = 8;  // 6~12 사이 튜닝

    private final Semaphore embedSemaphore = new Semaphore(MAX_EMBED_CONCURRENCY);
    private final ObjectMapper objectMapper = new ObjectMapper();

    //pdf 경로 모두 읽어 리소스 목록 생성
    //각 리소스를 processPdf()에 넘겨 비동기 실행
    public String createEmbeddingsFromAllPdfs() {
        try {
            System.out.println("⭐ 전체 PDF 문서 임베딩 시작");

            Resource[] etf = resourcePatternResolver.getResources("classpath*:static/etf/**/*.pdf");
            Resource[] tdf = resourcePatternResolver.getResources("classpath*:static/tdf/**/*.pdf");
            Resource[] resources = Stream.concat(Arrays.stream(etf), Arrays.stream(tdf))
                    .toArray(Resource[]::new);

            if (resources.length == 0) {
                System.out.println("⚠️ static/etf, static/tdf 폴더에 임베딩할 PDF 파일이 없습니다.");
                return "임베딩할 PDF 파일이 없습니다.";
            }

            List<CompletableFuture<String>> futures = Arrays.stream(resources)
                    .map(this::processPdf)
                    .collect(Collectors.toList());
            //모든 작업 완료 대기하고 성공 카운트 집계/로그
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            long successCount = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .count();

            String message = String.format("✨ 임베딩 완료: 파일 %d개 중 %d개 성공.", resources.length, successCount);
            System.out.println(message);
            return message;
        } catch (Exception e) {
            e.printStackTrace();
            return "임베딩 중 오류: " + e.getMessage();
        }
    }

    //이건 바로 실행말고 다른 스레드풀에 맡겨야지(병렬 실행)
    //스프링이 실행하는게 아니라 스레드풀에 처리해달라고 작업 지시
    //파일 1개 처리
    @Async("taskExecutor")
    public CompletableFuture<String> processPdf(Resource resource) {
        String fileNameWithRelativePath = null;
        try {
            String absolutePath = URLDecoder.decode(resource.getURL().getPath(), StandardCharsets.UTF_8);
            String[] split = absolutePath.split("static/");
            if (split.length < 2) {
                return CompletableFuture.completedFuture(null);
            }
            fileNameWithRelativePath = split[1].replace("\\", "/");

            // Oracle에 이미 저장된 chunkSeq 리스트 (중복 스킵)
            Set<Long> existingSeqs = new HashSet<>(
            		//기존 청크 시퀀스 가져옴
                    embeddingChunkRepository.findChunkSeqsByFileName(fileNameWithRelativePath)
            );

            System.out.println("📄 텍스트 추출: " + fileNameWithRelativePath);
            //본문 읽기+텍스트 추출
            String pdfText = fileService.readPdfText(fileNameWithRelativePath);
            if (pdfText == null || pdfText.isBlank()) {
                System.out.println("⚠️ 빈 텍스트, 건너뜀: " + fileNameWithRelativePath);
                return CompletableFuture.completedFuture(null);
            }
            //제어문자+공백 정리(추출한 텍스트 정리)
            String normalized = normalizeText(pdfText);
            //길이 1500,오버랩 50으로 쪼개기
            //청크 생성(중복 제거+결정적 UUID+메타데이터 포함)
            List<Document> allDocs = chunkToDocuments(normalized, fileNameWithRelativePath);
            if (allDocs.isEmpty()) {
                System.out.println("⚠️ 유효 청크 없음, 건너뜀: " + fileNameWithRelativePath);
                return CompletableFuture.completedFuture(null);
            }

            // 기존 청크 스킵(Oracle 기준)
            List<Document> newDocs = allDocs.stream()
                    .filter(d -> !existingSeqs.contains((Long) d.getMetadata().get("chunkSeq")))
                    .collect(Collectors.toList());

            if (newDocs.isEmpty()) {
                System.out.println("✅ 이미 완료(Oracle 기준): " + fileNameWithRelativePath);
                return CompletableFuture.completedFuture("Skipped: " + fileNameWithRelativePath);
            }

            // ✅ 직접 임베딩 + 직접 배치 insert
            System.out.println("⚡ pgvector 직접 배치 insert 시도... (" + newDocs.size() + "건) for " + fileNameWithRelativePath);
            long t0 = System.currentTimeMillis();
            //입장권 하나를 받는 동작
            //현재 사용 중인 스레드 수가 8보다 적으면 → 바로 통과
            //이미 8개가 실행 중이면 → 대기하다가 빈자리가 나면 입장(입장 인원 제한)->임베딩 API 한번에 많이 부르면 호출 에러
            embedSemaphore.acquire();
            try {
            	//세마포어 획득->newDocs를 BATCH_ADD_SIZE로 나눠 
            	//embedAndInsertBatchDirect(batch) 호출->세마포어 반환
                for (int start = 0; start < newDocs.size(); start += BATCH_ADD_SIZE) {
                    int end = Math.min(start + BATCH_ADD_SIZE, newDocs.size());
                    List<Document> batch = newDocs.subList(start, end);
                    embedAndInsertBatchDirect(batch);
                }
            } finally {
                embedSemaphore.release();
            }
            long t1 = System.currentTimeMillis();
            System.out.println("✅ pgvector 저장 완료 (direct), elapsed=" + (t1 - t0) + "ms, file=" + fileNameWithRelativePath);

            // Oracle 메타 저장
            final String finalFileName = fileNameWithRelativePath;
            List<EmbeddingChunk> chunks = newDocs.stream()
                    .map(doc -> EmbeddingChunk.builder()
                            .chunkSeq((Long) doc.getMetadata().get("chunkSeq"))
                            .chunkText(doc.getText())
                            .vectorId(doc.getId())
                            .fileName(finalFileName)
                            .createdAt(new Date())
                            .build())
                    .collect(Collectors.toList());
            //새 청크들만 일괄 저장
            embeddingChunkRepository.saveAll(chunks);
            System.out.println("✔ Oracle 저장 완료! for " + finalFileName);

            return CompletableFuture.completedFuture("Success: " + finalFileName);

        } catch (Exception e) {
            System.err.printf("❌ 처리 실패: %s -> %s%n", fileNameWithRelativePath, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
    private void embedAndInsertBatchDirect(List<Document> batch) throws JsonProcessingException {
    	//이번 배치에 포함된 모든 청크 텍스트만 뽑아서 리스트로 만듦(임베딩API에 한번에 보낼거기 때문에)
        List<String> texts = batch.stream().map(Document::getText).toList();
        // 1) 배치 임베딩 호출: 텍스트 여러 개를 한 번의 API 요청으로 임베딩합니다.
        var vectors = embeddingModel.embed(texts); // List<float[]> 또는 List<List<Double>>
        
        //안전장치: 임베딩 결과 개수와 입력 문서 개수가 다르면 바로 중단.
        if (vectors == null || vectors.size() != batch.size()) {
            throw new IllegalStateException(
                "Embedding size mismatch: " + (vectors == null ? 0 : vectors.size()) + " vs " + batch.size()
            );
        }

        // 2) PostgreSQL 배치 
        //ON CONFLICT (id) DO NOTHING: 같은 id가 이미 있으면 무시 → 중복 저장 방지.
        final String sql = "INSERT INTO embedding (id, content, metadata, embedding) " +
                "VALUES (?::uuid, ?, ?::jsonb, ?) ON CONFLICT (id) DO NOTHING";
        //벡터 DB 테이블 embedding(id, content, metadata, embedding)에 일괄 insert.
        // 멀티-VALUES INSERT INTO ... VALUES (),(),(),(),();->한번에 처리
        
        //원래 vectorstore.add는 INSERT INTO VALUES ();,INSERT INTO VALUES ();,INSERT INTO VALUES ();
        //이런식으로 INSERT됨
        pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Document doc = batch.get(i);
                Object vec = vectors.get(i); // ✅ 바로 꺼내서 사용 (float[] or List<?>)

                ps.setString(1, doc.getId());
                ps.setString(2, doc.getText());
                try {
                    ps.setString(3, objectMapper.writeValueAsString(doc.getMetadata()));
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new SQLException("metadata JSON serialize failed", e);
                }

                PGobject vectorObject = new PGobject();
                vectorObject.setType("vector");
                vectorObject.setValue(toVectorLiteral(vec)); // "[0.12,0.34,...]"
                ps.setObject(4, vectorObject);
            }
            @Override
            public int getBatchSize() { return batch.size(); }
        });

    }
    //제어문자/중복 공백 제거/줄바꿈 정리->청크 품질 개선 및 토큰 낭비 방지
    private String normalizeText(String s) {
        String cleaned = s.replaceAll("[\\u0000-\\u001F\\u007F]", " ");
        cleaned = cleaned.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        cleaned = cleaned.replaceAll("(?m)\\s*\\n\\s*", "\n");
        return cleaned.trim();
    }
    //텍스트를 청크1500 오버랩50으로 분할
    private List<Document> chunkToDocuments(String text, String fileNameWithRelativePath) {
        List<Document> docs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int len = text.length();
        int i = 0;
        long seq = 0;

        String lower = fileNameWithRelativePath.toLowerCase();
        String productCodePrefix = lower.contains("/tdf/") ? "TDF" :
                (lower.contains("/etf/") ? "ETF" : "UNKNOWN");

        while (i < len) {
            int end = Math.min(i + CHUNK_SIZE, len);
            String chunkText = text.substring(i, end).trim();
            if (!chunkText.isEmpty() && seen.add(chunkText)) {
                // 결정적 UUID: 파일경로 + seq + 길이 기반
                String key = fileNameWithRelativePath + ":" + seq + ":" + chunkText.length();
                UUID uuid = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("fileName", fileNameWithRelativePath);
                metadata.put("chunkSeq", seq);
                metadata.put("productCodePrefix", productCodePrefix);

                docs.add(new Document(uuid.toString(), chunkText, metadata));
                seq++;
            }
            int delta = end - i;
            int overlap = Math.min(CHUNK_OVERLAP, delta);
            int next = end - overlap;
            if (next <= i) next = end;
            i = next;
        }
        return docs;
    }

    // pgvector 벡터 텍스트 리터럴: [0.123,0.456,...]
    private String toVectorLiteral(Object vec) {
        StringBuilder sb = new StringBuilder("[]");
        if (vec == null) return "[]";

        sb.setLength(1); // '['만 남김
        if (vec instanceof double[] da) {
            for (int i = 0; i < da.length; i++) { if (i>0) sb.append(','); sb.append(trim(da[i])); }
        } else if (vec instanceof float[] fa) {
            for (int i = 0; i < fa.length; i++) { if (i>0) sb.append(','); sb.append(trim(fa[i])); }
        } else if (vec instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) { if (i>0) sb.append(','); sb.append(trim(((Number)list.get(i)).doubleValue())); }
        } else {
            throw new IllegalArgumentException("Unknown embedding vector type: " + vec);
        }
        return sb.append(']').toString();
    }

    private String trim(double v) { // 불필요한 0 제거
        String s = Double.toString(v);
        if (s.indexOf('E') >= 0 || s.indexOf('e') >= 0) return s; // 과학표기 유지
        if (s.indexOf('.') < 0) return s;
        int end = s.length();
        while (end > 0 && s.charAt(end-1) == '0') end--;
        if (end > 0 && s.charAt(end-1) == '.') end--;
        return s.substring(0, end);
    }
}
