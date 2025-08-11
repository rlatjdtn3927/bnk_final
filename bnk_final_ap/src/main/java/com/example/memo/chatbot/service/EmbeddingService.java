package com.example.memo.chatbot.service;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.EmbeddingChunk;
import com.example.memo.jpa.repository.EmbeddingChunkRepository;

/**
 * PDF → 텍스트 추출 → 청크 분할 → 임베딩 생성 → pgvector/Oracle 저장
 * - pgvector: 실제 벡터와 메타데이터(Document.id, metadata...) 저장 (PgVectorStore.add)
 * - Oracle  : 문서 청크 원문/메타를 보존 (replay/디버깅/추적)
 *
 * FileService는 "파일 경로를 받아 텍스트를 추출"하는 기존 구현을 사용한다고 가정.
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingChunkRepository embeddingChunkRepository;
    private final PgVectorStore pgVectorStore;               // ✅ pgvector 연동 VectorStore
    private final FileService fileService;                   // ✅ PDF 텍스트 추출용 (이미 구현돼 있다고 가정)
    private final ResourcePatternResolver resourcePatternResolver;
    private final JdbcTemplate pgJdbcTemplate;               // ✅ pgvector용 JDBC (UUID 존재 확인에 사용)

    /** 기본 청크 크기(문자 단위). PDF 특성상 400~800 근처 권장 */
    private static final int CHUNK_SIZE = 500;
    /** 문맥 보존을 위한 오버랩 */
    private static final int CHUNK_OVERLAP = 50;

    /**
     * classpath:static 하위 모든 PDF를 스캔하여 임베딩/저장.
     * - 이미 Oracle과 pgvector 모두에 동일 파일 벡터가 있으면 스킵.
     */
    @Transactional
    public String createEmbeddingsFromAllPdfs() throws IOException {
        System.out.println("⭐ 전체 PDF 문서 임베딩 시작");

        // classpath* 사용: 여러 모듈/리소스 위치에서도 탐색되도록
        Resource[] resources = resourcePatternResolver.getResources("classpath*:static/**/*.pdf");
        if (resources.length == 0) {
            System.out.println("⚠️ static 폴더에 임베딩할 PDF 파일이 없습니다.");
            return "임베딩할 PDF 파일이 없습니다.";
        }

        List<String> processedFiles = new ArrayList<>();
        int totalProcessedChunks = 0;

        for (Resource resource : resources) {
            // OS 경로/인코딩 차이를 최소화, static/ 이후 상대경로만 보존
            String absolutePath = URLDecoder.decode(resource.getURL().getPath(), StandardCharsets.UTF_8);
            String[] split = absolutePath.split("static/");
            if (split.length < 2) {
                System.out.println("⚠️ 경로 파싱 실패, 건너뜀: " + absolutePath);
                continue;
            }
            String fileNameWithRelativePath = split[1].replace("\\", "/");

            // 1) 이미 저장되어 있는지(Oracle+pgvector) 검사
            boolean existsInOracle = embeddingChunkRepository.existsByFileName(fileNameWithRelativePath);
            List<String> existingVectorIds = existsInOracle
                    ? embeddingChunkRepository.findVectorIdsByFileName(fileNameWithRelativePath)
                    : Collections.emptyList();

            boolean existsInPgvector = !existingVectorIds.isEmpty() && checkVectorIdsExistInPgvector(existingVectorIds);

            if (existsInOracle && existsInPgvector) {
                System.out.println("✅ 이미 존재: " + fileNameWithRelativePath + " (Oracle & pgvector)");
                continue; // 중복 작업 방지
            }

            // 2) 텍스트 추출
            System.out.println("📄 텍스트 추출: " + fileNameWithRelativePath);
            String pdfText = fileService.readPdfText(fileNameWithRelativePath);
            if (pdfText == null) pdfText = "";
            pdfText = normalizeText(pdfText);
            System.out.println("📄 추출된 텍스트 길이: " + pdfText.length());

            if (pdfText.isBlank()) {
                System.out.println("⚠️ 빈 텍스트, 건너뜀: " + fileNameWithRelativePath);
                continue;
            }

            // 3) 청크 분리(오버랩 적용)
            List<Document> documents = chunkToDocuments(pdfText, fileNameWithRelativePath);
            if (documents.isEmpty()) {
                System.out.println("⚠️ 유효 청크 없음, 건너뜀: " + fileNameWithRelativePath);
                continue;
            }

            // 4) pgvector 저장 (PgVectorStore가 임베딩 생성 → embedding 테이블 insert)
            System.out.println("💾 pgVectorStore 저장 시도... (" + documents.size() + "건)");
            try {
                pgVectorStore.add(documents);
                System.out.println("✅ pgVectorStore 저장 성공!");
            } catch (Exception e) {
                System.out.println("❌ pgVectorStore 저장 실패: " + e.getMessage());
                e.printStackTrace();
                // 이 파일은 건너뛰고 다음 파일 진행
                continue;
            }

            // 5) Oracle 저장 (메타데이터 기록: 검색/디버깅/추적용)
            System.out.println("📦 Oracle 메타 저장...");
            List<EmbeddingChunk> chunks = new ArrayList<>(documents.size());
            for (Document document : documents) {
                EmbeddingChunk chunk = EmbeddingChunk.builder()
                        .chunkSeq((Long) document.getMetadata().get("chunkSeq"))
                        .chunkText(document.getText())
                        .vectorId(document.getId())                // Document.id ← pgvector의 UUID와 매핑
                        .fileName(fileNameWithRelativePath)
                        .createdAt(new Date())
                        .build();
                chunks.add(chunk);
            }
            embeddingChunkRepository.saveAll(chunks);
            chunks.forEach(c ->
                    System.out.println("✔ Oracle 저장 완료 → seq: " + c.getChunkSeq() + ", vectorId: " + c.getVectorId())
            );

            processedFiles.add(fileNameWithRelativePath);
            totalProcessedChunks += documents.size();
        }

        String message = "✨ 임베딩 완료: 파일 " + processedFiles.size() + "개, 청크 " + totalProcessedChunks + "개.";
        System.out.println(message);
        return message;
    }

    /** 제어문자/중복 공백 정리, 줄바꿈 최소화 */
    private String normalizeText(String s) {
        String cleaned = s.replaceAll("[\\u0000-\\u001F\\u007F]", " ");    // 제어문자 제거
        cleaned = cleaned.replaceAll("[ \\t\\x0B\\f\\r]+", " ");            // 중복 공백
        cleaned = cleaned.replaceAll("(?m)\\s*\\n\\s*", "\n");              // 라인 앞뒤 여백
        return cleaned.trim();
    }

    /**
     * 텍스트 → 오버랩 포함 청크 → Document 리스트로 변환
     * - Document.id: UUID (pgvector id와 동일)
     * - metadata.fileName / metadata.chunkSeq 저장
     * - 중복 chunkText는 Set으로 제거(의미 없는 반복 방지)
     */
    private List<Document> chunkToDocuments(String text, String fileNameWithRelativePath) {
        List<Document> docs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        int len = text.length();
        int i = 0;
        long seq = 0;

        while (i < len) {
            int end = Math.min(i + CHUNK_SIZE, len);
            String chunkText = text.substring(i, end).trim();
            if (!chunkText.isEmpty() && seen.add(chunkText)) {
                String vectorId = UUID.randomUUID().toString();

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("fileName", fileNameWithRelativePath);
                metadata.put("chunkSeq", seq);

                docs.add(new Document(vectorId, chunkText, metadata));
                System.out.println("📌 청크 생성: ID=" + vectorId + " | seq=" + seq + " | len=" + chunkText.length());
                seq++;
            }

            // 다음 인덱스 계산 (마지막 구간에서 오버랩이 전체 길이를 잠식하지 않도록 보호)
            int delta = end - i;                                     // 이번에 소비한 길이
            int overlap = Math.min(CHUNK_OVERLAP, delta);            // 실제 적용 오버랩
            int next = end - overlap;
            if (next <= i) next = end;                               // 무한루프 방지
            i = next;
        }
        return docs;
    }

    /**
     * pgvector.embedding 테이블에 주어진 UUID(id)가 모두 존재하는지 확인.
     * - PostgreSQL의 UUID는 문자열과 직접 비교 불가 → id::text 캐스팅 필요
     */
    private boolean checkVectorIdsExistInPgvector(List<String> vectorIds) {
        if (vectorIds.isEmpty()) return false;

        String placeholders = vectorIds.stream().map(v -> "?").collect(Collectors.joining(","));
        String sql = "SELECT COUNT(*) FROM embedding WHERE id::text IN (" + placeholders + ")";

        Integer count = pgJdbcTemplate.queryForObject(sql, vectorIds.toArray(), Integer.class);
        return count != null && count == vectorIds.size();
    }
}
