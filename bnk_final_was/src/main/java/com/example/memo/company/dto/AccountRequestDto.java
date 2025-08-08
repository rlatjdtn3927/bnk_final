package com.example.memo.company.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AccountRequestDto {
	private List<String> memberIds;
}
