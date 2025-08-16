package com.example.memo.admin.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.memo.admin.dto.UpdatedSetDto;
import com.example.memo.jpa.entity.purchase.analysis.FileTaskList;
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

    @Transactional
    public UpdatedSetDto findUpdate() {
    	UpdatedSetDto dto = new UpdatedSetDto();
    	dto.setFundMasterList(fundMasterRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	dto.setFundDocList(fundDocumentRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	dto.setPrincipalList(principalGuaranteeRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	dto.setPrincipalDocList(principalDocumentRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	dto.setFundNavList(fundNavRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	dto.setFundReturnList(fundReturnRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	dto.setFundStatusList(fundStatusRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	dto.setRiskMetricList(riskMetricRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	dto.setCumulativePerformanceList(cumulativePerformanceRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	dto.setAssetAllocationList(assetAllocationRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	dto.setTop5SectorList(top5SectorRepository.findByStatusIn(Arrays.asList("INSERT", "UPDATE")));
    	return dto;
    }
    
    public Map<String, List<FileTaskList>> findAllFileTask() {
    	return Map.of("responseList",fileTaskListRepository.findAll());
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
    		String fileName = task.getProdName();
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
