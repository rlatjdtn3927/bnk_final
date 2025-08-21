package com.example.memo.purchase.common.service;


import java.util.*;
import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.purchase.analysis.CumulativePerformance;
import com.example.memo.jpa.entity.purchase.analysis.FundDocument;
import com.example.memo.jpa.entity.purchase.analysis.PrincipalDocument;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.repository.purchase.analysis.FundDocumentRepository;
import com.example.memo.jpa.repository.purchase.analysis.PrincipalDocumentRepository;
import com.example.memo.jpa.repository.purchase.commodity.FundMasterRepository;
import com.example.memo.jpa.repository.purchase.commodity.PrincipalGuaranteeRepository;
import com.example.memo.purchase.common.dto.FileUrlDto;
import com.example.memo.purchase.common.dto.FundCumulativeDto;
import com.example.memo.purchase.reserve.dto.request.ProductRequestDto;
import com.example.memo.purchase.reserve.dto.response.ProductResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.RequiredArgsConstructor;

/**
 * 상품 검색 서비스
 * - category: FUND / ETF / TDF / PRINCIPAL / CASH
 * - q(선택): 상품명/코드 키워드
 *
 * 응답: [{ productId, productName, category }]
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ObjectMapper mapper;
    private final FundMasterRepository fundRepo;
    private final PrincipalGuaranteeRepository principalRepo;
    private final FundDocumentRepository fundDocumentRepo;
    private final PrincipalDocumentRepository principalDocumentRepo;

    @Transactional
    public JsonNode searchProducts(JsonNode req) {
        JsonNode response = null;
        try {
            ProductRequestDto requestDto = mapper.treeToValue(req, ProductRequestDto.class);
            final String category = requestDto.getCategory();
            final String sortedBy = requestDto.getSortedBy();
            
            System.out.println("ap: " + requestDto.toString() + ".............................");
            if ("pg".equals(category)) {
            	Sort sort = Sort.by(sortedBy).descending(); // 엔티티 필드명과 매핑된 컬럼기준 내림차순 정렬
                Pageable pageable = PageRequest.of(
                    requestDto.getPage(),   // 요청 페이지 번호
                    requestDto.getSize(),  // 페이지 크기
                    sort
                );

                Page<PrincipalGuarantee> pageResult = principalRepo.findAll(pageable);
                List<String> productIdList = pageResult.getContent().stream().map(e -> e.getProductId()).toList();
                
                List<PrincipalDocument> docList = principalDocumentRepo.findByPrincipal_ProductIdIn(productIdList);
                List<FileUrlDto> urlResult = docList.stream()
                	    .map(e -> {
                	        PrincipalGuarantee principal = e.getPrincipal();
                	        return FileUrlDto.builder()
                	                .docType(e.getDocType())
                	                .fileUrl(e.getFileUrl())
                	                .prodId(principal.getProductId())
                	                .prodName(principal.getBankName() + " " + principal.getProductName() + " " + principal.getMaturityYears())
                	                .build();
                	    })
                	    .toList();
                
                response = mapper.valueToTree(new ProductResponseDto<PrincipalGuarantee>(pageResult,urlResult));  
            } else {
            	final Integer riskGradeNum = requestDto.getRiskGradeNum();
            	final String periodCode = requestDto.getPeriodCode();
                Pageable pageable = PageRequest.of(
                        requestDto.getPage(),   // 요청 페이지 번호
                        requestDto.getSize()  // 페이지 크기
                );

                Page<FundMaster> pageResult =
                	    fundRepo.findByCategoryAndRiskGradeNumAndPeriodCodeOrderByFundReturnDesc(
                	        category, riskGradeNum, periodCode, pageable);
                
                System.out.println("FundMaster page result size: " + pageResult.getContent().size());
            	Page<FundCumulativeDto> dtoPage = pageResult.map(fund -> {
            	    CumulativePerformance latest = fund.getCumulativePerformances().stream()
            	        .filter(cp -> periodCode.equals(cp.getPeriodCode()))
            	        .max(Comparator.comparing(CumulativePerformance::getReferenceDate))
            	        .orElse(null);

            	    return FundCumulativeDto.builder()
            	        .productId(fund.getProductId())
            	        .productName(fund.getProductName())
            	        .riskGradeNum(fund.getRiskGradeNum())
            	        .riskGradeText(fund.getRiskGradeText())
            	        .fundType(fund.getFundType())
            	        .inceptionDate(fund.getInceptionDate())
            	        .managementCompany(fund.getManagementCompany())
            	        .totalExpenseRatio(fund.getTotalExpenseRatio())
            	        .category(fund.getCategory())
            	        .periodCode(latest != null ? latest.getPeriodCode() : null)
            	        .fundReturn(latest != null ? latest.getFundReturn() : null)
            	        .build();
            	});
            	
            	System.out.println("FundCumulativeDto page result size: " + dtoPage.getContent().size());
                List<String> productIdList = pageResult.getContent().stream().map(e -> e.getProductId()).toList();
                List<FundDocument> docList = fundDocumentRepo.findByFund_ProductIdIn(productIdList);
                List<FileUrlDto> urlResult = docList.stream()
                	    .map(e -> {
                	        FundMaster fund = e.getFund();
                	        return FileUrlDto.builder()
                	                .docType(e.getDocType())
                	                .fileUrl(e.getFileUrl())
                	                .prodId(fund.getProductId())
                	                .prodName(fund.getProductName())
                	                .build();
                	    })
                	    .toList();
                response = mapper.valueToTree(new ProductResponseDto<FundCumulativeDto>(dtoPage,urlResult));  
            }

        } catch (JsonProcessingException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        return response;
    }
}