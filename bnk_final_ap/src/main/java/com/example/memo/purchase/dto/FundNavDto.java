package com.example.memo.purchase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundNavDto {
    private Long fundNavId;
    private String productId;     // FundMaster.productId
    private BigDecimal nav;
    private LocalDate referenceDate;
    private String status;
}