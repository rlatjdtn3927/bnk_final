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
import com.example.memo.purchase.dto.analysis.AssetAllocationDto;
import com.example.memo.purchase.dto.analysis.CumulativePerformanceDto;
import com.example.memo.purchase.dto.analysis.FundDocumentDto;
import com.example.memo.purchase.dto.analysis.FundNavDto;
import com.example.memo.purchase.dto.analysis.FundReturnDto;
import com.example.memo.purchase.dto.analysis.FundStatusDto;
import com.example.memo.purchase.dto.analysis.PrincipalDocumentDto;
import com.example.memo.purchase.dto.analysis.RiskMetricDto;
import com.example.memo.purchase.dto.analysis.Top5SectorDto;
import com.example.memo.purchase.dto.commodity.FundMasterDto;
import com.example.memo.purchase.dto.commodity.PrincipalGuaranteeDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatedSetDto {
	private List<FundMasterDto> fundMasterList;
	private List<FundDocumentDto> fundDocList;
	private List<PrincipalGuaranteeDto> principalList;
	private List<PrincipalDocumentDto> principalDocList;
	
	private List<FundStatusDto> fundStatusList;
	private List<FundReturnDto> fundReturnList;
	private List<FundNavDto> fundNavList;
	private List<AssetAllocationDto> assetAllocationList;
	private List<CumulativePerformanceDto> cumulativePerformanceList;
	private List<Top5SectorDto> top5SectorList;
	private List<RiskMetricDto> riskMetricList;
}
