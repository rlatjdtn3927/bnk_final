package com.example.memo.purchase.change.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SoldPrincipalDto {
	private Long id; //원리금 보장상품의 PK가 아니라 예금보유상품 principalLedger.id ->pk를 was에서 받아 ap로 보내자
	private Integer ratio;
}
