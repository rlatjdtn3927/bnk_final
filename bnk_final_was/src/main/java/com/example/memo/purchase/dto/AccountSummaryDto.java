package com.example.memo.purchase.dto;

import lombok.AllArgsConstructor; 
import lombok.Data;

@Data 
@AllArgsConstructor
public class AccountSummaryDto {
    private String accountName;
    private String accountNo;
    private long evalAmount;
    private double yieldRate;
}
