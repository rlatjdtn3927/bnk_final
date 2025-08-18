package com.example.memo.ocr.dto;

public record FamilyCertOcrRequest(
    String base64Data,            // PDF를 Base64로 인코딩한 문자열
    String format,                // "PDF"
    String expectedApplicantName  // 검증용: 신청인 성명(비교 대상)
) {}
