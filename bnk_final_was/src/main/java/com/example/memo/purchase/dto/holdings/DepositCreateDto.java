package com.example.memo.purchase.dto.holdings;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DepositCreateDto {
    private String accountType;
    private String acountId;
    private BigDecimal amount;
    private String description;               // 메모
    private LocalDateTime depositDate;        // 없으면 서버에서 now()
}
