package com.example.memo.admin.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static java.util.Collections.emptyList;

import com.example.memo.admin.dto.UpdatedSetDto;
import com.example.memo.jpa.entity.purchase.analysis.AssetAllocation;
import com.example.memo.jpa.entity.purchase.analysis.CumulativePerformance;
import com.example.memo.jpa.entity.purchase.analysis.FileTaskList;
import com.example.memo.jpa.entity.purchase.analysis.FundDocument;
import com.example.memo.jpa.entity.purchase.analysis.FundNav;
import com.example.memo.jpa.entity.purchase.analysis.FundReturn;
import com.example.memo.jpa.entity.purchase.analysis.FundStatus;
import com.example.memo.jpa.entity.purchase.analysis.PrincipalDocument;
import com.example.memo.jpa.entity.purchase.analysis.RiskMetric;
import com.example.memo.jpa.entity.purchase.analysis.Top5Sector;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.repository.admin.FileTaskListRepository;
import com.example.memo.jpa.repository.purchase.analysis.AssetAllocationRepository;
import com.example.memo.jpa.repository.purchase.analysis.CumulativePerformanceRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundDocumentRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundNavRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundReturnRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundStatusRepository;
import com.example.memo.jpa.repository.purchase.analysis.PrincipalDocumentRepository;
import com.example.memo.jpa.repository.purchase.analysis.RiskMetricRepository;
import com.example.memo.jpa.repository.purchase.analysis.Top5SectorRepository;
import com.example.memo.jpa.repository.purchase.commodity.FundMasterRepository;
import com.example.memo.jpa.repository.purchase.commodity.PrincipalGuaranteeRepository;
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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final FundMasterRepository fundMasterRepository;
    private final PrincipalGuaranteeRepository principalGuaranteeRepository;
    private final PrincipalDocumentRepository principalDocumentRepository;
    private final FundDocumentRepository fundDocumentRepository;
    
    private final FundStatusRepository fundStatusRepository;
    private final FundNavRepository fundNavRepository;
    private final FundReturnRepository fundReturnRepository;
    private final AssetAllocationRepository assetAllocationRepository;
    private final RiskMetricRepository riskMetricRepository;
    private final Top5SectorRepository top5SectorRepository;
    private final CumulativePerformanceRepository cumulativePerformanceRepository;
    
    private final FileTaskListRepository fileTaskListRepository;
    
    private ObjectMapper mapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public UpdatedSetDto findUpdate() {
        UpdatedSetDto dto = new UpdatedSetDto();

        // 1) 상태 기반 목록들 → DTO 매핑
        dto.setFundMasterList(
            fundMasterRepository.findByStatusIn(List.of("INSERT", "UPDATE"))
                .stream()
                .map(this::toFundMasterDto)
                .collect(Collectors.toList())
        );

        dto.setFundDocList(
            fundDocumentRepository.findByStatusIn(List.of("INSERT", "UPDATE"))
                .stream()
                .map(this::toFundDocumentDto)
                .collect(Collectors.toList())
        );

        dto.setPrincipalList(
            principalGuaranteeRepository.findByStatusIn(List.of("INSERT", "UPDATE"))
                .stream()
                .map(this::toPrincipalGuaranteeDto)
                .collect(Collectors.toList())
        );
        
        dto.setPrincipalDocList(
        		principalDocumentRepository.findByStatusIn(List.of("INSERT", "UPDATE"))
                .stream()
                .map(this::toPrincipalDocumentDto)
                .collect(Collectors.toList())
        );

        
        LocalDate latestNavDate = fundNavRepository.findTopByOrderByReferenceDateDesc()
                .map(FundNav::getReferenceDate)     
                .orElse(null);
        dto.setFundNavList(latestNavDate == null ? emptyList() :
            fundNavRepository.findByReferenceDate(latestNavDate)
                .stream()
                .map(this::toFundNavDto)
                .collect(Collectors.toList())
        );

        LocalDate latestReturnDate = fundReturnRepository.findTopByOrderByReferenceDateDesc()
                .map(FundReturn::getReferenceDate)   
                .orElse(null);
        dto.setFundReturnList(latestReturnDate == null ? emptyList() :
            fundReturnRepository.findByReferenceDate(latestReturnDate)
                .stream()
                .map(this::toFundReturnDto)
                .collect(Collectors.toList())
        );

        LocalDate latestStatusDate = fundStatusRepository.findTopByOrderByReferenceDateDesc()
                .map(FundStatus::getReferenceDate)   
                .orElse(null);
        dto.setFundStatusList(latestStatusDate == null ? emptyList() :
            fundStatusRepository.findByReferenceDate(latestStatusDate)
                .stream()
                .map(this::toFundStatusDto)
                .collect(Collectors.toList())
        );

        LocalDate latestRiskDate = riskMetricRepository.findTopByOrderByReferenceDateDesc()
                .map(RiskMetric::getReferenceDate)   
                .orElse(null);
        dto.setRiskMetricList(latestRiskDate == null ? emptyList() :
            riskMetricRepository.findByReferenceDate(latestRiskDate)
                .stream()
                .map(this::toRiskMetricDto)
                .collect(Collectors.toList())
        );

        LocalDate latestCumulDate = cumulativePerformanceRepository.findTopByOrderByReferenceDateDesc()
                .map(CumulativePerformance::getReferenceDate)
                .orElse(null);
        dto.setCumulativePerformanceList(latestCumulDate == null ? emptyList() :
            cumulativePerformanceRepository.findByReferenceDate(latestCumulDate)
                .stream()
                .map(this::toCumulativePerformanceDto)
                .collect(Collectors.toList())
        );

        LocalDate latestAssetDate = assetAllocationRepository.findTopByOrderByReferenceDateDesc()
                .map(AssetAllocation::getReferenceDate)
                .orElse(null);
        dto.setAssetAllocationList(latestAssetDate == null ? emptyList() :
            assetAllocationRepository.findByReferenceDate(latestAssetDate)
                .stream()
                .map(this::toAssetAllocationDto)
                .collect(Collectors.toList())
        );

        LocalDate latestTop5Date = top5SectorRepository.findTopByOrderByReferenceDateDesc()
                .map(Top5Sector::getReferenceDate)            
                .orElse(null);
        dto.setTop5SectorList(latestTop5Date == null ? emptyList() :
            top5SectorRepository.findByReferenceDate(latestTop5Date)
                .stream()
                .map(this::toTop5SectorDto)
                .collect(Collectors.toList())
        );
        
        return dto;
    }

    private FundMasterDto toFundMasterDto(FundMaster f) {
        return FundMasterDto.builder()
                .productId(f.getProductId())
                .productName(f.getProductName())
                .riskGradeNum(f.getRiskGradeNum())
                .riskGradeText(f.getRiskGradeText())
                .fundType(f.getFundType())
                .inceptionDate(f.getInceptionDate())
                .managementCompany(f.getManagementCompany())
                .totalExpenseRatio(f.getTotalExpenseRatio())
                .category(f.getCategory())
                .channel(f.getChannel())
                .status(f.getStatus())
                .build();
    }

    private FundDocumentDto toFundDocumentDto(FundDocument d) {
        return FundDocumentDto.builder()
                .productId(d.getFund().getProductId())
                .docType(d.getDocType())
                .fileUrl(d.getFileUrl())
                .status(d.getStatus())
                .build();
    }

    private PrincipalGuaranteeDto toPrincipalGuaranteeDto(PrincipalGuarantee p) {
        return PrincipalGuaranteeDto.builder()
                .productId(p.getProductId())
                .bankName(p.getBankName())
                .productName(p.getProductName())
                .maturityYears(p.getMaturityYears())
                .dbRate(p.getDbRate())
                .dcRate(p.getDcRate())
                .irpRate(p.getIrpRate())
                .status(p.getStatus())
                .build();
    }

    private PrincipalDocumentDto toPrincipalDocumentDto(PrincipalDocument d) {
        return PrincipalDocumentDto.builder()
                .productId(d.getPrincipal().getProductId())
                .docType(d.getDocType())
                .fileUrl(d.getFileUrl())
                .status(d.getStatus())
                .build();
    }

    private FundNavDto toFundNavDto(FundNav n) {
        return FundNavDto.builder()
                .productId(n.getFund().getProductId())
                .nav(n.getNav())
                .referenceDate(n.getReferenceDate())
                .status(n.getStatus())
                .build();
    }

    private FundReturnDto toFundReturnDto(FundReturn r) {
        return FundReturnDto.builder()
                .productId(r.getFund().getProductId())
                .returnType(r.getReturnType())
                .returnValue(r.getReturnValue())
                .referenceDate(r.getReferenceDate())
                .status(r.getStatus())
                .build();
    }

    private FundStatusDto toFundStatusDto(FundStatus s) {
        return FundStatusDto.builder()
                .productId(s.getFund().getProductId())
                .valuationType(s.getValuationType())
                .managementCompany(s.getManagementCompany())
                .investArea(s.getInvestArea())
                .trackRecord1y(s.getTrackRecord1y())
                .trackRecord2y(s.getTrackRecord2y())
                .expenseRatio1y(s.getExpenseRatio1y())
                .expenseRatio3y(s.getExpenseRatio3y())
                .launchDate(s.getLaunchDate())
                .salesFee(s.getSalesFee())
                .managementScale(s.getManagementScale())
                .trustFee(s.getTrustFee())
                .netAsset(s.getNetAsset())
                .redemptionFee(s.getRedemptionFee())
                .referenceDate(s.getReferenceDate())
                .status(s.getStatus())
                .build();
    }

    private RiskMetricDto toRiskMetricDto(RiskMetric m) {
        return RiskMetricDto.builder()
                .productId(m.getFund().getProductId())
                .metricName(m.getMetricName())
                .periodCode(m.getPeriodCode())
                .metricValue(m.getMetricValue())
                .percentileRank(m.getPercentileRank())
                .categoryAvg(m.getCategoryAvg())
                .referenceDate(m.getReferenceDate())
                .status(m.getStatus())
                .build();
    }

    private CumulativePerformanceDto toCumulativePerformanceDto(CumulativePerformance c) {
        return CumulativePerformanceDto.builder()
                .productId(c.getFund().getProductId())
                .periodCode(c.getPeriodCode())
                .fundReturn(c.getFundReturn())
                .bmReturn(c.getBmReturn())
                .categoryAvgRet(c.getCategoryAvgRet())
                .referenceDate(c.getReferenceDate())
                .status(c.getStatus())
                .build();
    }

    private AssetAllocationDto toAssetAllocationDto(AssetAllocation a) {
        return AssetAllocationDto.builder()
                .productId(a.getFund().getProductId())
                .allocCategory(a.getAllocCategory())
                .inFund(a.getInFund())
                .categoryAvg(a.getCategoryAvg())
                .referenceDate(a.getReferenceDate())
                .status(a.getStatus())
                .build();
    }

    private Top5SectorDto toTop5SectorDto(Top5Sector s) {
        return Top5SectorDto.builder()
                .productId(s.getFund().getProductId())
                .sectorCategory(s.getSectorCategory())
                .inStock(s.getInStock())
                .categoryAvg(s.getCategoryAvg())
                .referenceDate(s.getReferenceDate())
                .status(s.getStatus())
                .build();
    }
    
    public Map<String, List<FileTaskList>> findAllFileTask() {
    	List<FileTaskList> list = fileTaskListRepository.findAll();
    	System.out.println("FILE TASK LIST SIZE : " + list.size());
    	return Map.of("responseList",list);
    }
    
    public Map<String, Boolean> downloadFiles(JsonNode data) {
    	
        List<String> prodIdList = mapper.convertValue(
                data.get("prodIdList"), new TypeReference<List<String>>() {}
            );
    	
    	List<FileTaskList> taskList = fileTaskListRepository.findByProdIdIn(prodIdList);
    	Map<String, Boolean> resultMap = new HashMap<>();
    	
    	if(taskList == null) return null;
    	
    	for(FileTaskList task : taskList) {
    		String fileURL = task.getDownloadUrl();
    		String category = task.getProdCategory();
    		String fileName = task.getFileName();
    		boolean isSuccess = true;
    		
    		try {
    			downloadFile(fileURL, category, fileName);
    			fileTaskListRepository.delete(task); //다운로드 완료 후 태스크 삭제.
    		} catch (Exception e) {
    			isSuccess = false;
    		}
    		if(fileURL != null) {
    			resultMap.put(task.getProdId(), isSuccess);
    		}
    	}
    	
    	return resultMap;
    }
    
    //파일 다운로드 유틸리티 함수
    private void downloadFile(String fileURL, String category, String fileName) throws IOException {
        Path base = Paths.get("C:\\bnk_project");
        Path targetPath = base.resolve(category).resolve(fileName);
        Files.createDirectories(targetPath.getParent());

        String encodedUrl = fileURL.replace(" ", "%20"); //띄어쓰기 인코딩
        URL url = new URL(encodedUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
