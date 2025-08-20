package com.example.memo.irp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AccountContractResult {
	private Long joinId;
	private String irpAcctNo;	// 신규 생성된 IRP 계좌번호
	private String contractNo;	// 신규 생성된 IRP 계약번호
}
