package com.example.memo.ocr.dto;

public record FamilyCertOcrResponse(
    String docType,           // 문서명
    String applicantName,     // 본인명
    String applicantRrn,  // 본인 주민번호
    String spouseName,        // 배우자명
    String spouseRrn,     // 배우자 주민번호
    boolean verified,         // 인증 통과 여부
    String reason             // 실패 사유(verified=false일 때)
) {}