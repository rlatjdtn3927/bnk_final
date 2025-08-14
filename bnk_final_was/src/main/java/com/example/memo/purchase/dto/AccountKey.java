package com.example.memo.purchase.dto;

import lombok.Data;

@Data
public class AccountKey {
	private String accountType; //IRP OR DC
	private String accountId;	//IRP:irp_acct_no DC:dc_account.id
}
