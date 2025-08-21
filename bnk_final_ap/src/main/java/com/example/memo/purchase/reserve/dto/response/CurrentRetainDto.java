package com.example.memo.purchase.reserve.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentRetainDto {
    private String productId;
    private String productName;
    private String fundType;
    private String riskGradeText;
    private Integer retainRatio;
}