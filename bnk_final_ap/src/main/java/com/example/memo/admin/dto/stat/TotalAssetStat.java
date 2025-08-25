package com.example.memo.admin.dto.stat;

import java.math.BigDecimal;

//총자산 (IRP/DC별)
public record TotalAssetStat(
 BigDecimal cashTotal,       // 계좌 balance 합
 BigDecimal principalTotal,  // (contractAmount + interestAccrued) 합
 BigDecimal fundTotal,       // valuationAmount 합
 BigDecimal totalAsset       // 위 3개 합
) {}