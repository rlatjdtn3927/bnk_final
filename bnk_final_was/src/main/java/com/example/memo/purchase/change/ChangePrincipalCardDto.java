package com.example.memo.purchase.change;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangePrincipalCardDto {
    private String principalId;       // 예금/정기예금 상품 ID
    private String productName;       // 예: "퇴직연금 정기예금 1년제"

    private Long contractAmount;      // 매수원금(=계약금액)
    private Long valuationAmount;     // 평가액(계약금액+미지급이자 등 계산 시 사용)
    private Long sellableAmount;      // 매도가능금액(통상 계약금액과 동일 취급)

    private BigDecimal interestRate;  // 금리(%) - 단일/범위 표기 시 문자열로 바꿔도 무방
    private LocalDate startDate;
    private LocalDate maturityDate;
}
