package com.example.memo.couple.ocr.dto;

import lombok.Data;

@Data
public class SpouseOcrRequest {
    /** 파일 본문(Base64, 개행 없음) */
    private String base64;
    /** 원본 파일명(포맷 추정에 사용) */
    private String filename;
    /** Content-Type (예: application/pdf, image/png) */
    private String contentType;
}