package com.example.memo.purchase.reserve.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductRequestDto {
	private String category; //원리금 보장 상품: pg | etf: etf, tdf : tdf, fund : fund
	private Integer riskGradeNum; // etf, tdf, fund인 경우에만
	private Integer page;
	private Integer size;
	private String sortedBy; // 디폴트: 원리금 보장상품일 경우에는 bankName, 펀드들 일 때는 null
	private String periodCode; // 펀드들일 경우 수익률 정렬 기준 (1,3,6개월, 1년)
}
