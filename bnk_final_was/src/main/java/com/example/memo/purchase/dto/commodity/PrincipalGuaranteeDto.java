package com.example.memo.purchase.dto.commodity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrincipalGuaranteeDto {
    private String productId;
    private String bankName;
    private String productName;
    private String maturityYears;
    private BigDecimal dbRate;
    private BigDecimal dcRate;
    private BigDecimal irpRate;
    private String status;    // "PENDING", "INSERTED", "UPDATED"
}