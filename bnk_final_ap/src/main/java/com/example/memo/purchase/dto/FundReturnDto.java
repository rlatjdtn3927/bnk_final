package com.example.memo.purchase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundReturnDto {
    private Long returnId;
    private String productId;     // FundMaster.productId
    private String returnType;    // 펀드, 유형평균
    private BigDecimal returnValue;
    private LocalDate referenceDate;
    private String status;
}