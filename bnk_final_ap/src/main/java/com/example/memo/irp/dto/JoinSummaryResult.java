package com.example.memo.irp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class JoinSummaryResult {
	private Long joinId;
    private String branchOffice;    // 영업점
    private Long annualContribAmt;  // 연간 납입 한도 (Long)
    private Long newContribAmt;     // 신규 입금 금액 (Long)
}
