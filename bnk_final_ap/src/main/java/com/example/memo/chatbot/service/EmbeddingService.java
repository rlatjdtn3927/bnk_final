package com.example.memo.chatbot.service;

import java.io.IOException;
import java.util.*;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.EmbeddingChunk;
import com.example.memo.jpa.repository.EmbeddingChunkRepository;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingChunkRepository embeddingChunkRepository;
    private final PgVectorStore pgVectorStore;
    private final FileService fileService;
    private final ResourcePatternResolver resourcePatternResolver;

    @Transactional
    public String createEmbeddingsFromAllPdfs() throws IOException, JsonProcessingException {
        System.out.println("⭐ 전체 PDF 문서 임베딩 시작");

        Resource[] resources = resourcePatternResolver.getResources("classpath:static/**/*.pdf");

        if (resources.length == 0) {
            System.out.println("⚠️ static 폴더에 임베딩할 PDF 파일이 없습니다.");
            return "임베딩할 PDF 파일이 없습니다.";
        }

        List<String> processedFiles = new ArrayList<>();
        int totalProcessedChunks = 0;

        for (Resource resource : resources) {
            String fileNameWithRelativePath = resource.getURL().getPath().split("static/")[1];

            // 이미 임베딩된 파일은 건너뛰기
            if (embeddingChunkRepository.existsByFileName(fileNameWithRelativePath)) {
                System.out.println("✅ 파일(" + fileNameWithRelativePath + ")은 이미 임베딩되었습니다. 건너뜁니다.");
                continue;
            }

            System.out.println("📄 파일(" + fileNameWithRelativePath + ") 텍스트 추출 중...");
            String pdfText = fileService.readPdfText(fileNameWithRelativePath);
            System.out.println("📄 추출된 텍스트 길이: " + pdfText.length());

            if (pdfText == null || pdfText.trim().isEmpty()) {
                System.out.println("⚠️ 텍스트 추출 실패! 이 파일은 건너뜁니다.");
                continue;
            }

            int chunkSize = 500;
            List<Document> documents = new ArrayList<>();
            int chunkCount = (int) Math.ceil((double) pdfText.length() / chunkSize);

            for (int i = 0; i < pdfText.length(); i += chunkSize) {
                String chunkText = pdfText.substring(i, Math.min(i + chunkSize, pdfText.length())).trim();

                if (chunkText.isEmpty()) {
                    System.out.println("⚠️ 빈 청크 발견! 건너뜁니다.");
                    continue;
                }

                String vectorId = UUID.randomUUID().toString();

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("fileName", fileNameWithRelativePath);
                metadata.put("chunkSeq", (long) (i / chunkSize));

                Document document = new Document(vectorId, chunkText, metadata);
                System.out.println("📌 청크 생성: ID=" + vectorId + " | 텍스트 길이=" + chunkText.length());
                documents.add(document);
            }

            if (documents.isEmpty()) {
                System.out.println("⚠️ 유효한 청크가 없어 저장을 건너뜁니다.");
                continue;
            }

            // 임베딩 저장
            System.out.println("💾 pgVectorStore 임베딩 저장 시도...");
            try {
                pgVectorStore.add(documents);
                System.out.println("✅ pgVectorStore 저장 성공! 문서 수: " + documents.size());
            } catch (Exception e) {
                System.out.println("❌ pgVectorStore 저장 실패: " + e.getMessage());
                e.printStackTrace();
                continue; // 실패한 파일은 스킵
            }

            // Oracle에 청크 메타데이터 저장
            System.out.println("📦 Oracle DB에 메타데이터 저장 중...");
            for (Document document : documents) {
                EmbeddingChunk chunk = EmbeddingChunk.builder()
                        .chunkSeq((Long) document.getMetadata().get("chunkSeq"))
                        .chunkText(document.getText())
                        .vectorId(document.getId())
                        .fileName(fileNameWithRelativePath)
                        .createdAt(new Date())
                        .build();
                embeddingChunkRepository.save(chunk);
                System.out.println("✔ Oracle 저장 완료 → chunkSeq: " + chunk.getChunkSeq() + ", vectorId: " + chunk.getVectorId());
            }

            processedFiles.add(fileNameWithRelativePath);
            totalProcessedChunks += documents.size();
        }

        String message = "✨ 전체 임베딩 완료! 총 " + processedFiles.size() + "개의 파일이 처리되었으며, 총 " + totalProcessedChunks + "개의 청크가 저장되었습니다.";
        System.out.println(message);
        return message;
    }
}
