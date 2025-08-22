package com.example.memo.purchase.reserve.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReserveUpdateRequestDto {
	private Long userId;
	private String accountType;
	private List<String> targetProdIdList;
	private List<SourceProdRatioDto> sourceProdList;
}
