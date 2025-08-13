package com.example.memo.irp.dto;

import lombok.Data;

@Data
public class ContractUpdateDto {
	private Long annualContribAmt;
	private Long newContribAmt;
	private String acctNo;		 // 출금계좌
	private String acctPwd;       // ★ 출금계좌 비밀번호 (평문, 현재 요구사항)
	private String branchOffice; // 관리영업점
}
