package com.example.memo.purchase.change.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundHoldingsDto {

    private Long id;

    private String irpAccountId;   // IrpAccount FK -> id
    private String dcAccountNo;  // DcAccount FK -> account_no
    private String fundProductId; // FundMaster FK -> product_id

    // FundMaster 정보
    private String productName;
    private Byte riskGradeNum;
    private String riskGradeText;
    private String fundType;
    private LocalDate inceptionDate;
    private String managementCompany;
    private BigDecimal totalExpenseRatio;
    private String category;     // fund, etf, tdf
    private Integer channel;
    private String status;       // "INSERTED", "UPDATED"

    // 보유 정보
    private BigDecimal units;             // 보유 좌수
    private BigDecimal avgPrice;          // 평균 단가
    private BigDecimal acquisitionAmount; // 매수 원금
    private BigDecimal valuationAmount;   // 평가액
    private BigDecimal profitLoss;        // 평가손익
    private BigDecimal returnRate;        // 수익률
}