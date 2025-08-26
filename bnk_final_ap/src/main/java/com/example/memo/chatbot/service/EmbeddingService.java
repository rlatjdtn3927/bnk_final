// src/main/java/com/example/memo/chatbot/service/EmbeddingService.java
package com.example.memo.chatbot.service;

import com.example.memo.jpa.entity.chatbot.EmbeddingChunk;
import com.example.memo.jpa.repository.chatbot.EmbeddingChunkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.postgresql.util.PGobject;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingChunkRepository embeddingChunkRepository;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate pgJdbcTemplate;
    private final FileService fileService;

    private static final int CHUNK_SIZE = 1500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int BATCH_ADD_SIZE = 64;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 전체 PDF → Oracle + pgvector 동기화 임베딩
     */
    public String createEmbeddingsFromAllPdfs() {
        System.out.println("⭐ 임베딩 전체 파이프라인 시작");

        try {
            List<String> baseDirs = List.of(
                    "/pdf/fund/",
                    "/pdf/etf/",
                    "/pdf/tdf/",
                    "/pdf/pg/",
                    "/pdf/default/"
            );

            Map<String, String> allTexts = new LinkedHashMap<>();
            for (String baseDir : baseDirs) {
                Map<String, String> map = fileService.readAllPdfTexts(baseDir);
                System.out.printf("📂 디렉토리 [%s] -> 읽은 PDF 수: %d%n", baseDir, map.size());
                allTexts.putAll(map);
            }

            if (allTexts.isEmpty()) {
                System.out.println("⚠️ 읽은 PDF 텍스트가 없음. 종료");
                return "⚠️ 임베딩할 PDF가 없습니다.";
            }

            long successCount = 0;
            int totalFiles = allTexts.size();
            int currentFileNum = 0;

            for (Map.Entry<String, String> entry : allTexts.entrySet()) {
                currentFileNum++;
                String fileName = entry.getKey();
                String text = entry.getValue();

                System.out.printf("%n-------------------- [ %d / %d ] --------------------%n", currentFileNum, totalFiles);
                System.out.println("📄 처리 시작: " + fileName);

                if (text == null || text.isBlank()) {
                    System.out.println("⚠️ 빈 텍스트, 스킵: " + fileName);
                    continue;
                }

                try {
                    // 1. Oracle에서 기존 청크 정보 조회
                    Set<Long> existingOracleSeqs = new HashSet<>(
                            embeddingChunkRepository.findChunkSeqsByFileName(fileName)
                    );
                    System.out.println("  [1/5] Oracle DB 기존 청크 조회: " + existingOracleSeqs.size() + "건");

                    // 2. 텍스트 정규화 및 청크 분할
                    String normalized = normalizeText(text);
                    List<Document> allDocs = chunkToDocuments(normalized, fileName);
                    System.out.println("  [2/5] 텍스트 청크 분할 완료: " + allDocs.size() + "개");

                    if (allDocs.isEmpty()) {
                        System.out.println("  ⚠️ 청크가 생성되지 않아 스킵합니다.");
                        continue;
                    }

                    // 3. 각 청크의 DB 존재 여부 확인 및 처리할 목록 필터링
                    List<Document> docsToProcess = new ArrayList<>();
                    int syncedCount = 0;
                    for (Document doc : allDocs) {
                        Long seq = (Long) doc.getMetadata().get("chunkSeq");
                        boolean inOracle = existingOracleSeqs.contains(seq);
                        boolean inPg = existsInPgVector(doc.getId());

                        if (inOracle && inPg) {
                            syncedCount++;
                        } else {
                            // 나중에 Oracle에 저장할지 여부를 메타데이터에 기록
                            doc.getMetadata().put("needsOracleSave", !inOracle);
                            docsToProcess.add(doc);
                        }
                    }

                    System.out.printf("  [3/5] 청크 상태 분석: 동기화 완료 %d개, 처리 필요 %d개%n", syncedCount, docsToProcess.size());

                    if (docsToProcess.isEmpty()) {
                        System.out.println("  ✅ 모든 청크가 이미 동기화되어 있습니다. 다음 파일로 넘어갑니다.");
                        successCount++; // 처리할 것이 없어도 성공으로 간주
                        continue;
                    }

                    // 4. 임베딩 생성 및 pgvector DB에 저장
                    System.out.printf("  [4/5] %d개 청크에 대한 임베딩 생성 및 pgvector 저장 시작...%n", docsToProcess.size());
                    for (int start = 0; start < docsToProcess.size(); start += BATCH_ADD_SIZE) {
                        int end = Math.min(start + BATCH_ADD_SIZE, docsToProcess.size());
                        List<Document> batch = docsToProcess.subList(start, end);
                        System.out.printf("    - 배치 처리 중: %d ~ %d (총 %d개)%n", start, end - 1, batch.size());

                        List<String> texts = batch.stream().map(Document::getText).toList();
                        var vectors = embeddingModel.embed(texts);

                        if (vectors == null || vectors.size() != batch.size()) {
                            throw new IllegalStateException("Embedding 결과 오류: expected=" + batch.size() + " got=" + (vectors == null ? "null" : vectors.size()));
                        }

                        final String sql = "INSERT INTO embedding (id, content, metadata, embedding) VALUES (?::uuid, ?, ?::jsonb, ?) ON CONFLICT (id) DO NOTHING";
                        int[] results = pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                Document doc = batch.get(i);
                                Object vec = vectors.get(i);
                                ps.setString(1, doc.getId());
                                ps.setString(2, doc.getText());
                                ps.setString(3, toJson(doc.getMetadata()));
                                PGobject vectorObject = new PGobject();
                                vectorObject.setType("vector");
                                vectorObject.setValue(toVectorLiteral(vec));
                                ps.setObject(4, vectorObject);
                            }

                            @Override
                            public int getBatchSize() { return batch.size(); }
                        });
                        System.out.printf("    - 배치 저장 완료 (pgvector): %d건 시도, %d건 성공%n", batch.size(), Arrays.stream(results).sum());
                    }
                    System.out.println("  ✅ [4/5] pgvector 저장 완료.");


                    // 5. Oracle DB에 메타데이터 저장 (필요한 경우)
                    List<EmbeddingChunk> newChunksForOracle = docsToProcess.stream()
                            .filter(doc -> (boolean) doc.getMetadata().getOrDefault("needsOracleSave", false))
                            .map(doc -> EmbeddingChunk.builder()
                                    .chunkSeq((Long) doc.getMetadata().get("chunkSeq"))
                                    .chunkText(doc.getText())
                                    .vectorId(doc.getId())
                                    .fileName(fileName)
                                    .createdAt(new Date())
                                    .build())
                            .toList();

                    if (!newChunksForOracle.isEmpty()) {
                        System.out.printf("  [5/5] Oracle DB에 신규 청크 메타데이터 저장 시작: %d건%n", newChunksForOracle.size());
                        embeddingChunkRepository.saveAll(newChunksForOracle);
                        System.out.println("  ✔ [5/5] Oracle DB 저장 완료.");
                    } else {
                        System.out.println("  ℹ️ [5/5] Oracle DB에 추가할 신규 청크가 없습니다.");
                    }

                    successCount++;
                    System.out.println("✨ 파일 처리 성공: " + fileName);

                } catch (Exception inner) {
                    System.err.println("❌ 처리 중 오류 발생: " + fileName);
                    inner.printStackTrace();
                }
            }
            
            System.out.println("\n-------------------- [ 최종 결과 ] --------------------");
            String msg = String.format("✨ 임베딩 완료: 총 파일 %d개 중 %d개 성공", totalFiles, successCount);
            System.out.println(msg);
            return msg;

        } catch (Exception e) {
            System.err.println("❌ 임베딩 전체 실행 중 오류");
            e.printStackTrace();
            return "임베딩 중 오류: " + e.getMessage();
        }
    }

    // ===== 유틸 =====

    private boolean existsInPgVector(String uuid) {
        String sql = "SELECT 1 FROM embedding WHERE id = ?::uuid LIMIT 1";
        List<Integer> res = pgJdbcTemplate.query(sql, ps -> ps.setString(1, uuid),
                (rs, rowNum) -> rs.getInt(1));
        return !res.isEmpty();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }

    private String normalizeText(String s) {
        String cleaned = s.replaceAll("[\\u0000-\\u001F\\u007F]", " ");
        cleaned = cleaned.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        cleaned = cleaned.replaceAll("(?m)\\s*\\n\\s*", "\n");
        return cleaned.trim();
    }

    private List<Document> chunkToDocuments(String text, String fileName) {
        List<Document> docs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int len = text.length();
        int i = 0;
        long seq = 0;

        String lower = fileName.toLowerCase();
        String prefix = lower.contains("/tdf/") ? "TDF" :
                (lower.contains("/etf/") ? "ETF" :
                        (lower.contains("/fund/") ? "FUND" :
                                (lower.contains("/pg/") ? "PG" : "UNKNOWN")));

        while (i < len) {
            int end = Math.min(i + CHUNK_SIZE, len);
            String chunkText = text.substring(i, end).trim();
            if (!chunkText.isEmpty() && seen.add(chunkText)) {
                String key = fileName + ":" + seq + ":" + chunkText.length();
                UUID uuid = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("fileName", fileName);
                metadata.put("chunkSeq", seq);
                metadata.put("productCodePrefix", prefix);

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

    private String toVectorLiteral(Object vec) {
        StringBuilder sb = new StringBuilder("[]");
        if (vec == null) return "[]";
        sb.setLength(1);
        if (vec instanceof double[] da) {
            for (int i = 0; i < da.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(trim(da[i]));
            }
        } else if (vec instanceof float[] fa) {
            for (int i = 0; i < fa.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(trim(fa[i]));
            }
        } else if (vec instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(trim(((Number) list.get(i)).doubleValue()));
            }
        }
        return sb.append(']').toString();
    }

    private String trim(double v) {
        String s = Double.toString(v);
        if (s.contains("E") || s.contains("e")) return s;
        if (!s.contains(".")) return s;
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') end--;
        if (end > 0 && s.charAt(end - 1) == '.') end--;
        return s.substring(0, end);
    }
}
