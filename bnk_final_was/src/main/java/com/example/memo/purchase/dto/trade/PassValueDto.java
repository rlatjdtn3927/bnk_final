package com.example.memo.purchase.dto.trade;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PassValueDto {
	/*전 스텝 공통*/
	private String flow;
	private String accountId; // 계좌식별 id (irp인 경우에는 irp_acct_no, DC인 경우에는 account_no) --> 테이블 접근용 데이터
	private String accountType; // IRP, DC
	/*전 스텝 공통*/
	
	/*Step 2에서 추가 : 보유상품 목록*/
	private String riskGrade; // profile_type 유형명
	private Integer riskGradeNum;
	private List<String> targetProdIdList;
	private String targetProdId; // 만기 변경 및 보유 변경 시 선택한 상품ID
	/*Step 2에서 추가*/
	
	/*Step 3에서 추가 : 상품목록*/
	private List<Map<String,Integer>> sourceProdIdList; // 상품목록에서 선택한 상품ID --> 1.매수예정 등록: 여러개 
	private String sourceProdId; // 2,3. 만기 및 변경: 1개
	private List<FileUrlDto> fileUrlList;
	/*Step 3에서 추가*/
	
}
