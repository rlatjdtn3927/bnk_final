package com.example.memo.purchase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetAllocationDto {
    private Long allocId;
    private String productId;     // FundMaster.productId
    private String allocCategory;
    private BigDecimal inFund;
    private BigDecimal categoryAvg;
    private LocalDate referenceDate;
    private String status;        // "INSERTED", "UPDATED"
}
