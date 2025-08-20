package com.example.memo.purchase.dto.trade;

import java.math.BigDecimal;
import java.time.LocalDate;

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
public class FundCumulativeDto {
    private String productId;
    private String productName;
    private Byte riskGradeNum;
    private String riskGradeText;
    private String fundType;
    private LocalDate inceptionDate;
    private String managementCompany;
    private BigDecimal totalExpenseRatio;
    private String category;  // fund, etf, tdf
    private String periodCode;
    private BigDecimal fundReturn;
}
