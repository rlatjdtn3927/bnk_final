package com.example.memo.purchase.trade_common.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeValueDto {
	
	/*전 스텝 공통*/
	   private String flow;
	   private String accountId; // 계좌식별 id (irp인 경우에는 irp_acct_no, DC인 경우에는 account_no) --> 테이블 접근용 데이터
	   private String accountType; // IRP, DC
	   /*전 스텝 공통*/
	   
	   /*Step 2에서 추가 : 보유상품 목록*/
	   private String riskGrade; // profile_type 유형명
	   private Integer riskGradeNum;
}
