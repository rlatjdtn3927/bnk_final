package com.example.memo.irp.dto;

import lombok.Data;

@Data
public class DraftJoinDto {
	private Long userId;	// TestUser에서 주입 (화면 입력 아님)
	private String joinPurpose;	// 세액공제 / 퇴직금수령
}
