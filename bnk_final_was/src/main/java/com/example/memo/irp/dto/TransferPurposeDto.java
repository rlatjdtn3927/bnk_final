package com.example.memo.irp.dto;

import lombok.Data;

@Data
public class TransferPurposeDto {
	private String prevBank;	//이전 금융기관명
    private String prevAccount;	//이전 계좌번호
    private Long transferAmt;	//이전 금액
    private String transferDoc;	//이전 증빙서류 경로 또는 파일명
}
