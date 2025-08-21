package com.example.memo.couple.ocr.dto;

import lombok.Data;

@Data
public class SpouseOcrResponse {
    /** 배우자 성명 (템플릿 필드 기반) */
    private String spouseName;
    /** 배우자 생년월일 yyyy-MM-dd (주민번호에서 변환) */
    private String spouseBirth;
    /** 배우자 관계 확인 여부(필드 유무로 판정) */
    private boolean spouseRelation;

    /** 부가 정보 (로그/디버깅 용) */
    private String docType;
    private String applicantName;
    private String applicantBirth;
    private String reason;       // 실패 사유(필요 시)
    private boolean ok;          // 파싱 자체 성공 여부
}