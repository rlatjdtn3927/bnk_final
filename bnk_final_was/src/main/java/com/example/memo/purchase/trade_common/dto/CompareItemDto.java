package com.example.memo.purchase.trade_common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompareItemDto {
    private String productId;
    private String productName;
    private String type;      // "PG" or "FUND"
    private Integer percent;  // RESERVE면 표시, 그 외 null 허용
}
