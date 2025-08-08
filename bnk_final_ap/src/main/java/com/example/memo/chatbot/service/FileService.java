package com.example.memo.chatbot.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class FileService {

	public String readPdfText(String encodedPath) {
	    try {
	        // 1. 경로 디코딩 (브라우저 인코딩 고려)
	        String decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8);

	        // 2. 리소스 경로 확인 로그
	        String fullPath = "static/" + decodedPath;
	        System.out.println("📄 [FileService] 읽는 중: " + fullPath);
	        ClassPathResource resource = new ClassPathResource(fullPath);

	        if (!resource.exists()) {
	            System.out.println("❌ 리소스를 찾을 수 없음: " + fullPath);
	            return ""; // 빈 텍스트 리턴
	        }

	        // 3. PDF 로드 및 텍스트 추출
	        try (InputStream inputStream = resource.getInputStream();
	             PDDocument document = PDDocument.load(inputStream)) {

	            if (document.isEncrypted()) {
	                System.out.println("🔒 암호화된 PDF, 건너뜁니다: " + fullPath);
	                return "";
	            }

	            PDFTextStripper pdfStripper = new PDFTextStripper();
	            String rawText = pdfStripper.getText(document);

	            // 4. 텍스트 정리
	            String cleanedText = rawText
	                .replace("\u0000", "")                            // null 문자 제거
	                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")       // 나머지 제어 문자 제거
	                .replaceAll(" +", " ")                           // 다중 공백 → 단일 공백
	                .trim();

	            cleanedText = cleanedText.replace("&", "&amp;")
	                                     .replace("<", "&lt;")
	                                     .replace(">", "&gt;");

	            // 로그 추가
	            System.out.println("✅ 추출 성공: " + decodedPath + " | 길이: " + cleanedText.length());
	            return cleanedText;
	        }

	    } catch (IOException e) {
	        System.out.println("❌ PDF 읽기 실패: " + encodedPath + " → " + e.getMessage());
	        return "";
	    } catch (Exception e) {
	        System.out.println("❌ 예외 발생: " + encodedPath + " → " + e.getMessage());
	        return "";
	    }
	}



}
