package com.example.memo.irp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JoinSummaryResult {
	private final String joinPurpose;     // 가입목적
    private final String branchOffice;    // 영업점
    private final Long annualContribAmt;  // 연간 납입 한도 (Long)
    private final Long newContribAmt;     // 신규 입금 금액 (Long)
}
