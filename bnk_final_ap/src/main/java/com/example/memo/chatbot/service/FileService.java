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
	        // 1. 경로 디코딩
	        String decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8);
	        ClassPathResource resource = new ClassPathResource("static/" + decodedPath);

	        // 2. PDF 로드
	        try (InputStream inputStream = resource.getInputStream();
	             PDDocument document = PDDocument.load(inputStream)) {

	            PDFTextStripper pdfStripper = new PDFTextStripper();
	            String rawText = pdfStripper.getText(document);

	            // 3. 문자 정리
	            String cleanedText = rawText
	                .replace("\u0000", "")                            // null 문자 제거
	                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")       // 나머지 제어 문자 제거
	                .replaceAll(" +", " ")                           // 다중 공백 → 단일 공백
	                .trim();
	             cleanedText = cleanedText.replace("&", "&amp;")
	                                      .replace("<", "&lt;")
	                                     .replace(">", "&gt;");

	            return cleanedText;
	        }

	    } catch (IOException e) {
	        throw new RuntimeException("PDF 파일을 읽을 수 없습니다: " + encodedPath, e);
	    }
	    
	}


}
