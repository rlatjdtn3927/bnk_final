package com.example.memo.purchase.dto.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class FundRetainDto {
    private String productId;
    private String productName;
    private String category;
    private String riskGradeText;
    private Integer retainRatio;
}