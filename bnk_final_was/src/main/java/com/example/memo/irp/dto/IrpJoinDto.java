package com.example.memo.irp.dto;

import lombok.Data;

@Data
public class IrpJoinDto {
	private Long userId;
    private String joinPurpose;
    private String productId;	// PK → ProductMaster 조회용
    private Long annualContribAmt;
    private Long newContribAmt;
    private String contractNo;
    private String acctNo;	// PK → BankAccount 조회용
}
