package com.example.memo.purchase.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Top5SectorDto {
    private Long sectorId;
    private String productId;     // FundMaster.productId
    private String sectorCategory;
    private BigDecimal inStock;
    private BigDecimal categoryAvg;
    private LocalDate referenceDate;
    private String status;
}