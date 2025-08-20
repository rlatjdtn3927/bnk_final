package com.example.memo.irp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InitOpenResponse {
	//IRP계좌번호/계약번호 생성 dto 
    private String contractNo;
    private String acctNo;
}
