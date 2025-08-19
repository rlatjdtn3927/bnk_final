package com.example.memo.purchase.dto.trade;

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
	private String prodId; //상품 id
    private String prodName;     // 상품 이름
    private String docType;    // 문서 타입 (예: 펀드: 투자설명서, 간이투자설명서, 상품약관 " 원리금: 약관, 상품설명서)
    private String fileUrl; // 저장 위치 url
}