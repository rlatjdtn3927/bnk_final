package com.example.memo.purchase.dto;

import lombok.AllArgsConstructor; 
import lombok.Data;

@Data 
@AllArgsConstructor
public class HoldingItemDto {
    private String category;
    private String name;
    private long amount;
    private double pnlRate; // 수익률 %
}
