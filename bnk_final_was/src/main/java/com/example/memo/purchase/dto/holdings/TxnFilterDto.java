package com.example.memo.purchase.dto.holdings;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TxnFilterDto {
    private String accountType;
    private String acountId;
    private LocalDate from;   // nullable
    private LocalDate to;     // nullable
}
