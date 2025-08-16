package com.example.memo.purchase.dto.commodity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundMasterDto {
    private String productId;
    private String productName;
    private Byte riskGradeNum;
    private String riskGradeText;
    private String fundType;
    private LocalDate inceptionDate;
    private String managementCompany;
    private BigDecimal totalExpenseRatio;
    private String category;  // fund, etf, tdf
    private Integer channel;
    private String status;
}