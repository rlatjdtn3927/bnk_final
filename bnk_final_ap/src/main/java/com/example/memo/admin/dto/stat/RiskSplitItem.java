package com.example.memo.admin.dto.stat;

import java.math.BigDecimal;

//위험/안전 비중
public record RiskSplitItem(
 String bucket,              // "SAFE","RISK"
 BigDecimal amount,
 BigDecimal percent
) {}