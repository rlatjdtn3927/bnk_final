package com.example.memo.purchase.change.dto;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SellLineDto {
    private String itemType;        // FUND | PRINCIPAL | CASH
    private String itemId;          // fundProductId | principalId | 'CASH'
    private String name;            // 화면표시용 상품명
    private BigDecimal availableAmount; // 매도가능금액(원) 기준
    private Integer ratio;          // 0..100 (null이면 0)
    private BigDecimal sellAmount;  // 계산된 매도금액(available * ratio/100)
}
