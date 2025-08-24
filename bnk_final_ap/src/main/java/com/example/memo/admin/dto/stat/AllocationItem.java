package com.example.memo.admin.dto.stat;

import java.math.BigDecimal;

//상품 비중 (현금 제외: PRINCIPAL / ETF / TDF / FUND)
public record AllocationItem(
 String category,            // "PRINCIPAL","ETF","TDF","FUND"
 BigDecimal amount,
 BigDecimal percent
) {}