package com.example.memo.purchase.dto.holdings;

import java.math.BigDecimal;

import lombok.Data;
//보유현황 탭
@Data
public class SummaryDto {
	private BigDecimal totalEvalAmt;     // 총 평가액
    private BigDecimal totalProfitRate;  // 수익률(%)
    private BigDecimal totalDepositAmt;  // 누적 입금액
    private BigDecimal todayDepositAmt;  // 당일 입금액
    private BigDecimal opProfitAmt;      // 운용 수익금액
}
