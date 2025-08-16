package com.example.memo.admin.dto;

import java.util.List;

import com.example.memo.jpa.entity.purchase.analysis.AssetAllocation;
import com.example.memo.jpa.entity.purchase.analysis.CumulativePerformance;
import com.example.memo.jpa.entity.purchase.analysis.FundDocument;
import com.example.memo.jpa.entity.purchase.analysis.FundNav;
import com.example.memo.jpa.entity.purchase.analysis.FundReturn;
import com.example.memo.jpa.entity.purchase.analysis.FundStatus;
import com.example.memo.jpa.entity.purchase.analysis.PrincipalDocument;
import com.example.memo.jpa.entity.purchase.analysis.RiskMetric;
import com.example.memo.jpa.entity.purchase.analysis.Top5Sector;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatedSetDto {
	private List<FundMaster> fundMasterList;
	private List<FundDocument> fundDocList;
	private List<PrincipalGuarantee> principalList;
	private List<PrincipalDocument> PrincipalDocList;
	private List<FundStatus> fundStatusList;
	private List<FundReturn> fundReturnList;
	private List<FundNav> FundNavList;
	private List<AssetAllocation> AssetAllocationList;
	private List<CumulativePerformance> cumulativePerformanceList;
	private List<Top5Sector> top5SectorList;
	private List<RiskMetric> RiskMetricList;
}
