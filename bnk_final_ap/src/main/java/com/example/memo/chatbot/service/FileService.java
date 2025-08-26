package com.example.memo.chatbot.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.net.URLDecoder;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class FileService {

    /**
     * 특정 디렉토리 안의 모든 PDF 파일을 읽어서 텍스트 추출
     * @param encodedDirPath 브라우저 등에서 전달된 경로 (예: "/pdf/etf/")
     * @return Map<파일명, 텍스트> 형태
     */
    public Map<String, String> readAllPdfTexts(String encodedDirPath) {
        Map<String, String> results = new LinkedHashMap<>();
        try {
            // 1. 경로 디코딩
            String decodedPath = URLDecoder.decode(encodedDirPath, StandardCharsets.UTF_8);

            // 2. FileMatchConfig에서 매핑된 실제 로컬 경로로 변환
            // "/pdf/etf/" → "C:/bnk_project/etf/"
            String baseDir;
            if (decodedPath.startsWith("/pdf/fund")) {
                baseDir = "C:/bnk_project/fund/";
            } else if (decodedPath.startsWith("/pdf/etf")) {
                baseDir = "C:/bnk_project/etf/";
            } else if (decodedPath.startsWith("/pdf/tdf")) {
                baseDir = "C:/bnk_project/tdf/";
            } else if (decodedPath.startsWith("/pdf/pg")) {
                baseDir = "C:/bnk_project/pg/";
            } else {
                baseDir = "C:/bnk_project/default/";
            }

            Path dir = Paths.get(baseDir);
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                System.out.println("❌ 디렉토리를 찾을 수 없음: " + baseDir);
                return results;
            }

            // 3. 디렉토리 내 모든 PDF 탐색
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.pdf")) {
                for (Path filePath : stream) {
                    String text = extractPdfText(filePath);
                    results.put(filePath.getFileName().toString(), text);
                }
            }

        } catch (Exception e) {
            System.out.println("❌ 전체 PDF 읽기 실패: " + e.getMessage());
        }
        return results;
    }

    /**
     * 단일 PDF 파일에서 텍스트 추출
     */
    private String extractPdfText(Path filePath) {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            if (document.isEncrypted()) {
                System.out.println("🔒 암호화된 PDF, 건너뜀: " + filePath);
                return "";
            }
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String rawText = pdfStripper.getText(document);

            String cleanedText = rawText
                    .replace("\u0000", "")
                    .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                    .replaceAll(" +", " ")
                    .trim();

            cleanedText = cleanedText.replace("&", "&amp;")
                                     .replace("<", "&lt;")
                                     .replace(">", "&gt;");

            System.out.println("✅ 추출 성공: " + filePath + " | 길이: " + cleanedText.length());
            return cleanedText;
        } catch (IOException e) {
            System.out.println("❌ PDF 추출 실패: " + filePath + " → " + e.getMessage());
            return "";
        }
    }
}
