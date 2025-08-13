package com.example.memo.irp.dto;

import lombok.Data;

@Data
public class TaxPurposeDto {
	private String irpQualType;	// 예: 근로자/자영업자
    private String businessNo;
}
