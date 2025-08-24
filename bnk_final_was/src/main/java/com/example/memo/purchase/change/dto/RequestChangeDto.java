package com.example.memo.purchase.change.dto;

import java.util.List;

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
public class RequestChangeDto {
	private String accountType;
	private String accountId;
	private List<SoldFundDto> soldProdList;
	private List<BuyFundDto> buyFundList;
	private List<SoldPrincipalDto> soldPrincipalIdList;
	private List<BuyPrincipalDto> buyPrincipalList;
	
}
