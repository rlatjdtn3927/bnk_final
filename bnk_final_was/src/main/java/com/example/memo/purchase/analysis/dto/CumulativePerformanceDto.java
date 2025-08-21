package com.example.memo.purchase.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CumulativePerformanceDto {
    private Long perfId;
    private String productId;     // FundMaster.productId
    private String periodCode;
    private BigDecimal fundReturn;
    private BigDecimal bmReturn;
    private BigDecimal categoryAvgRet;
    private LocalDate referenceDate;
    private String status;
}