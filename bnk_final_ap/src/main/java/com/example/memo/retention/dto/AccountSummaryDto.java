package com.example.memo.retention.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountSummaryDto {
    private String accountType;      // "IRP" | "DC"
    private String accountNo;        // irp_acct_no | dc.account_no
    private String displayName;      // 화면용 표기 (개인IRP / 퇴직연금 DC 등)
    private BigDecimal balance;      // 잔액/평가액 요약
    private String userName;         // 사용자 이름
    
    private BigDecimal totalProfitLoss; // 총 손익
    private BigDecimal returnRate;      // 총 수익률
}
