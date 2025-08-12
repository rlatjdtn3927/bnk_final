package com.example.memo.chatbot.service;

import com.example.memo.jpa.entity.EmbeddingChunk;
import com.example.memo.jpa.repository.EmbeddingChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingChunkRepository embeddingChunkRepository;
    private final PgVectorStore pgVectorStore;
    private final FileService fileService;
    private final ResourcePatternResolver resourcePatternResolver;
    private final JdbcTemplate pgJdbcTemplate;

    private static final int CHUNK_SIZE = 1500;
    private static final int CHUNK_OVERLAP = 50;

    @Transactional
    public String createEmbeddingsFromAllPdfs() throws IOException {
        System.out.println("⭐ 전체 PDF 문서 임베딩 시작");

        Resource[] resources = resourcePatternResolver.getResources("classpath*:static/**/*.pdf");
        if (resources.length == 0) {
            System.out.println("⚠️ static 폴더에 임베딩할 PDF 파일이 없습니다.");
            return "임베딩할 PDF 파일이 없습니다.";
        }

        List<CompletableFuture<String>> futures = Arrays.stream(resources)
                .map(resource -> processPdf(resource))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long successCount = futures.stream()
                .map(f -> f.join())
                .filter(Objects::nonNull)
                .count();

        String message = String.format("✨ 임베딩 완료: 파일 %d개 중 %d개 성공.", resources.length, successCount);
        System.out.println(message);
        return message;
    }

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

            boolean existsInOracle = embeddingChunkRepository.existsByFileName(fileNameWithRelativePath);
            List<String> existingVectorIds = existsInOracle ? embeddingChunkRepository.findVectorIdsByFileName(fileNameWithRelativePath) : Collections.emptyList();
            boolean existsInPgvector = !existingVectorIds.isEmpty() && checkVectorIdsExistInPgvector(existingVectorIds);

            if (existsInOracle && existsInPgvector) {
                System.out.println("✅ 이미 존재: " + fileNameWithRelativePath + " (Oracle & pgvector)");
                return CompletableFuture.completedFuture("Skipped: " + fileNameWithRelativePath);
            }

            System.out.println("📄 텍스트 추출: " + fileNameWithRelativePath);
            String pdfText = fileService.readPdfText(fileNameWithRelativePath);
            if (pdfText == null || pdfText.isBlank()) {
                System.out.println("⚠️ 빈 텍스트, 건너뜀: " + fileNameWithRelativePath);
                return CompletableFuture.completedFuture(null);
            }
            pdfText = normalizeText(pdfText);

            List<Document> documents = chunkToDocuments(pdfText, fileNameWithRelativePath);
            if (documents.isEmpty()) {
                System.out.println("⚠️ 유효 청크 없음, 건너뜀: " + fileNameWithRelativePath);
                return CompletableFuture.completedFuture(null);
            }

            System.out.println("💾 pgVectorStore 저장 시도... (" + documents.size() + "건) for " + fileNameWithRelativePath);

            Thread.sleep(100);

            pgVectorStore.add(documents);
            System.out.println("✅ pgVectorStore 저장 성공! for " + fileNameWithRelativePath);

            System.out.println("📦 Oracle 메타 저장... for " + fileNameWithRelativePath);

            // ✅ 람다식 내부에서 사용할 변수를 final로 선언하여 문제 해결
            final String finalFileName = fileNameWithRelativePath;

            List<EmbeddingChunk> chunks = documents.stream()
                    .map(doc -> EmbeddingChunk.builder()
                            .chunkSeq((Long) doc.getMetadata().get("chunkSeq"))
                            .chunkText(doc.getText())
                            .vectorId(doc.getId())
                            .fileName(finalFileName) // ✅ final 변수 사용
                            .createdAt(new Date())
                            .build())
                    .collect(Collectors.toList());

            embeddingChunkRepository.saveAll(chunks);
            System.out.println("✔ Oracle 저장 완료! for " + finalFileName);

            return CompletableFuture.completedFuture("Success: " + finalFileName);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("❌ 처리 실패 (Thread Interrupted): %s -> %s%n", fileNameWithRelativePath, e.getMessage());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            System.err.printf("❌ 처리 실패: %s -> %s%n", fileNameWithRelativePath, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    private String normalizeText(String s) {
        String cleaned = s.replaceAll("[\\u0000-\\u001F\\u007F]", " ");
        cleaned = cleaned.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        cleaned = cleaned.replaceAll("(?m)\\s*\\n\\s*", "\n");
        return cleaned.trim();
    }

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

    private boolean checkVectorIdsExistInPgvector(List<String> vectorIds) {
        if (vectorIds.isEmpty()) return false;
        String placeholders = vectorIds.stream().map(v -> "?").collect(Collectors.joining(","));
        String sql = "SELECT COUNT(*) FROM embedding WHERE id::text IN (" + placeholders + ")";
        Integer count = pgJdbcTemplate.queryForObject(sql, vectorIds.toArray(), Integer.class);
        return count != null && count == vectorIds.size();
    }
}