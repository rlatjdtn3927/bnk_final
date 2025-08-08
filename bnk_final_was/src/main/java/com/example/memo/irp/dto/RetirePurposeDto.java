package com.example.memo.irp.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class RetirePurposeDto {
	private LocalDate retireDate;	//퇴직일
    private String retireReason;	//퇴직사유
    private String corpName;		//퇴직 회사명
    private Long severanceAmt;		//퇴직금 수령액
    private String withholdDoc;		//원천징수영수증 파일경로
}
