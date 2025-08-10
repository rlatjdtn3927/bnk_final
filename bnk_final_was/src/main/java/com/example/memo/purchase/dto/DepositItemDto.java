package com.example.memo.purchase.dto;

import lombok.AllArgsConstructor; 
import lombok.Data;
import java.time.LocalDateTime;

@Data 
@AllArgsConstructor
public class DepositItemDto {
    private LocalDateTime at;
    private String type;   // 입금/출금
    private long amount;
}
