package com.example.memo.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class FileUrlDto {
    private String prodId;     // 상품 ID
    private String docType;    // 문서 타입 (예: 펀드: 투자설명서, 간이투자설명서, 상품약관 " 원리금: 약관, 상품설명서)
    private String downloadUrl; // 다운로드 URL
}