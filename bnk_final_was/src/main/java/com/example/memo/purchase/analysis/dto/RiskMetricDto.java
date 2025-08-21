package com.example.memo.purchase.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskMetricDto {
    private Long metricId;
    private String productId;     // FundMaster.productId
    private String metricName;
    private String periodCode;
    private BigDecimal metricValue;
    private BigDecimal percentileRank;
    private BigDecimal categoryAvg;
    private LocalDate referenceDate;
    private String status;
}