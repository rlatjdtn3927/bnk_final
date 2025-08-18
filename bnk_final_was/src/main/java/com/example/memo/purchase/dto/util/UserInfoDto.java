package com.example.memo.purchase.dto.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class UserInfoDto {
	String accountType;
	String userName;
	String accountNumber;
	String riskGrade;
	Integer riskGradeNum;
}
