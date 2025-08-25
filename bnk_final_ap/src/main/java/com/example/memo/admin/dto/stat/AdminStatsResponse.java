package com.example.memo.admin.dto.stat;

import java.util.List;
import java.util.Map;

//대시보드 응답
public record AdminStatsResponse(
 Map<String, TotalAssetStat> totalAssetsByType, // IRP, DC
 List<AllocationItem> allocation,               // 전체(현금 제외)
 List<RiskSplitItem> riskSplit                  // SAFE / RISK
) {}