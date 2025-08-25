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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.postgresql.util.PGobject;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingChunkRepository embeddingChunkRepository;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate pgJdbcTemplate;
    private final FileService fileService;

    // 청크 관련 파라미터
    private static final int CHUNK_SIZE = 1500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int BATCH_ADD_SIZE = 64;
    private static final int MAX_EMBED_CONCURRENCY = 8;

    private final Semaphore embedSemaphore = new Semaphore(MAX_EMBED_CONCURRENCY);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 전체 PDF 디렉토리 대상 임베딩 실행
     */
    public String createEmbeddingsFromAllPdfs() {
        try {
            System.out.println("⭐ 전체 PDF 문서 임베딩 시작");

            // FileService로 ETF/TDF/FUND/PG/DEFAULT 모두 스캔
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
                allTexts.putAll(map);
            }

            if (allTexts.isEmpty()) {
                return "⚠️ 임베딩할 PDF가 없습니다.";
            }

            // 각 파일 비동기 처리
            List<CompletableFuture<String>> futures = new ArrayList<>();
            for (Map.Entry<String, String> entry : allTexts.entrySet()) {
                futures.add(processPdf(entry.getKey(), entry.getValue()));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            long successCount = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .count();

            String message = String.format("✨ 임베딩 완료: 파일 %d개 중 %d개 성공.", allTexts.size(), successCount);
            System.out.println(message);
            return message;
        } catch (Exception e) {
            e.printStackTrace();
            return "임베딩 중 오류: " + e.getMessage();
        }
    }

    /**
     * 단일 PDF 처리
     */
    @Async("taskExecutor")
    public CompletableFuture<String> processPdf(String fileName, String text) {
        try {
            if (text == null || text.isBlank()) {
                System.out.println("⚠️ 빈 텍스트, 건너뜀: " + fileName);
                return CompletableFuture.completedFuture(null);
            }

            // Oracle에 이미 저장된 chunkSeq 조회
            Set<Long> existingSeqs = new HashSet<>(
                    embeddingChunkRepository.findChunkSeqsByFileName(fileName)
            );

            // 텍스트 정리
            String normalized = normalizeText(text);
            List<Document> allDocs = chunkToDocuments(normalized, fileName);
            if (allDocs.isEmpty()) {
                System.out.println("⚠️ 청크 없음, 건너뜀: " + fileName);
                return CompletableFuture.completedFuture(null);
            }

            // 신규 청크만 필터링
            List<Document> newDocs = allDocs.stream()
                    .filter(d -> !existingSeqs.contains((Long) d.getMetadata().get("chunkSeq")))
                    .toList();

            if (newDocs.isEmpty()) {
                System.out.println("✅ 이미 완료(Oracle 기준): " + fileName);
                return CompletableFuture.completedFuture("Skipped: " + fileName);
            }

            // 임베딩 + pgvector 저장
            embedSemaphore.acquire();
            try {
                for (int start = 0; start < newDocs.size(); start += BATCH_ADD_SIZE) {
                    int end = Math.min(start + BATCH_ADD_SIZE, newDocs.size());
                    embedAndInsertBatchDirect(newDocs.subList(start, end));
                }
            } finally {
                embedSemaphore.release();
            }

            // Oracle 저장
            List<EmbeddingChunk> chunks = newDocs.stream()
                    .map(doc -> EmbeddingChunk.builder()
                            .chunkSeq((Long) doc.getMetadata().get("chunkSeq"))
                            .chunkText(doc.getText())
                            .vectorId(doc.getId())
                            .fileName(fileName)
                            .createdAt(new Date())
                            .build())
                    .toList();
            embeddingChunkRepository.saveAll(chunks);

            System.out.println("✔ Oracle 저장 완료! for " + fileName);
            return CompletableFuture.completedFuture("Success: " + fileName);

        } catch (Exception e) {
            System.err.printf("❌ 처리 실패: %s -> %s%n", fileName, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 배치 임베딩 + pgvector insert
     */
    private void embedAndInsertBatchDirect(List<Document> batch) throws Exception {
        List<String> texts = batch.stream().map(Document::getText).toList();
        var vectors = embeddingModel.embed(texts);

        if (vectors == null || vectors.size() != batch.size()) {
            throw new IllegalStateException("Embedding size mismatch");
        }

        final String sql = "INSERT INTO embedding (id, content, metadata, embedding) " +
                "VALUES (?::uuid, ?, ?::jsonb, ?) ON CONFLICT (id) DO NOTHING";

        pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
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
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
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
        String productCodePrefix = lower.contains("/tdf/") ? "TDF" :
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

    private String toVectorLiteral(Object vec) {
        StringBuilder sb = new StringBuilder("[]");
        if (vec == null) return "[]";
        sb.setLength(1);
        if (vec instanceof double[] da) {
            for (int i = 0; i < da.length; i++) { if (i > 0) sb.append(','); sb.append(trim(da[i])); }
        } else if (vec instanceof float[] fa) {
            for (int i = 0; i < fa.length; i++) { if (i > 0) sb.append(','); sb.append(trim(fa[i])); }
        } else if (vec instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) { if (i > 0) sb.append(','); sb.append(trim(((Number) list.get(i)).doubleValue())); }
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
