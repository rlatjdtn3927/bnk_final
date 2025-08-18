package com.example.memo.purchase.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ModelValueDto {
	/*전 스텝 공통*/
	String flow;
	String accountId; // 계좌식별 id (irp인 경우에는 irp_acct_no, DC인 경우에는 account_no) --> 테이블 접근용 데이터
	String accountType; // IRP, DC
	/*전 스텝 공통*/
	
	/*Step 2에서 추가 : 보유상품 목록*/
	String riskGrade; // profile_type 유형명
	String riskGradeNum;
	String targetProdId; // 만기 변경 및 보유 변경 시 선택한 상품ID
	/*Step 2에서 추가*/
	
	/*Step 3에서 추가 : 상품목록*/
	List<String> sourceProdId; // 상품목록에서 선택한 상품ID --> 1. 매수예정 등록: 여러개 // 2,3. 만기 및 변경: 1개
	/*Step 3에서 추가*/
	
}
