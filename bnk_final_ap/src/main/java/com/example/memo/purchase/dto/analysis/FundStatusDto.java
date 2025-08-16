package com.example.memo.purchase.dto.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundStatusDto {
    private Long statusId;
    private String productId;         // FundMaster.productId
    private String valuationType;
    private String managementCompany;
    private String investArea;
    private String trackRecord1y;
    private String trackRecord2y;
    private String expenseRatio1y;
    private String expenseRatio3y;
    private String launchDate;
    private BigDecimal salesFee;
    private String managementScale;
    private BigDecimal trustFee;
    private String netAsset;
    private BigDecimal redemptionFee;
    private LocalDate referenceDate;
    private String status;
}