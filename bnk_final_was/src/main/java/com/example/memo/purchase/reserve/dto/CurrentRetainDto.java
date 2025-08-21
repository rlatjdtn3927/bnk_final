package com.example.memo.purchase.reserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class CurrentRetainDto {
    private String productId;
    private String productName;
    private String category;
    private String riskGradeText;
    private Integer retainRatio;
}