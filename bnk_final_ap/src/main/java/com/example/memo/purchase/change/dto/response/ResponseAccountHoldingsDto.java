package com.example.memo.purchase.change.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseAccountHoldingsDto {

    // 펀드 보유 내역
    private List<FundHoldingsDto> fundHoldings;

    // 원리금 보장상품 보유 내역
    private List<PrincipalLedgerDto> principalLedgers;
}