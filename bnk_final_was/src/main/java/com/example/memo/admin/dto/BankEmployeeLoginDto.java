package com.example.memo.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BankEmployeeLoginDto {
    private Long id;
	private String username;
    private String password;
}