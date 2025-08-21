package com.example.memo.purchase.change.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.ledger.FundHoldings;
import com.example.memo.jpa.entity.ledger.PrincipalLedger;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.repository.ledger.FundHoldingsRepository;
import com.example.memo.jpa.repository.ledger.PrincipalLedgerRepository;
import com.example.memo.purchase.change.dto.request.RequestRetainHoldingsDto;
import com.example.memo.purchase.change.dto.response.FundHoldingsDto;
import com.example.memo.purchase.change.dto.response.PrincipalLedgerDto;
import com.example.memo.purchase.change.dto.response.ResponseAccountHoldingsDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeProductService {
	
	private final FundHoldingsRepository fundHoldingsRepository;
	private final PrincipalLedgerRepository principalLedgerRepository;
	private final ObjectMapper mapper;
	
	@Transactional
	public ResponseAccountHoldingsDto getHoldings(JsonNode data) {
		try {
			RequestRetainHoldingsDto dto = mapper.treeToValue(data, RequestRetainHoldingsDto.class);
			String accountType = dto.getAccountType();
			String accountId = dto.getAccountId();
			List<FundHoldings> fundHoldingsList = null;
			List<PrincipalLedger> principalHoldingList = null;
			if("DC".equals(accountType)) {
				DcAccount proxy = DcAccount.builder().accountNo(accountId).build();
				fundHoldingsList = fundHoldingsRepository.findByDcAccount(proxy);
				principalHoldingList = principalLedgerRepository.findByDcAccount(proxy);
			} else { //irp
				IrpAccount proxy = IrpAccount.builder().irpAcctNo(accountId).build();
				fundHoldingsList = fundHoldingsRepository.findByIrpAccount(proxy);
				principalHoldingList = principalLedgerRepository.findByIrpAccount(proxy);
			}
			
			if (fundHoldingsList != null && principalHoldingList != null) {
			    List<FundHoldingsDto> fundHoldingsDtoList = fundHoldingsList.stream()
			        .map(e -> {
			            FundMaster fund = e.getFund();
			            return FundHoldingsDto.builder()
			            		.irpAccountId(e.getIrpAccount() != null ? e.getIrpAccount().getIrpAcctNo() : null)
			            		.dcAccountNo(e.getDcAccount() != null ? e.getDcAccount().getAccountNo() : null)
			            		
			                    .fundProductId(fund.getProductId())
			                    .productName(fund.getProductName())
			                    .riskGradeNum(fund.getRiskGradeNum())
			                    .riskGradeText(fund.getRiskGradeText())
			                    .fundType(fund.getFundType())
			                    .inceptionDate(fund.getInceptionDate())
			                    .managementCompany(fund.getManagementCompany())
			                    .totalExpenseRatio(fund.getTotalExpenseRatio())
			                    .category(fund.getCategory())

			                    .units(e.getUnits())
			                    .avgPrice(e.getAvgPrice())
			                    .acquisitionAmount(e.getAcquisitionAmount())
			                    .valuationAmount(e.getValuationAmount())
			                    .profitLoss(e.getProfitLoss())
			                    .returnRate(e.getReturnRate())
			            		.build();
			        }).toList();
			    List<PrincipalLedgerDto> principalLedgerDtoList = principalHoldingList.stream()
			    	    .map((PrincipalLedger e) -> {
			    	        return PrincipalLedgerDto.builder()
			    	            .id(e.getId())
			    	            .irpAccountId(e.getIrpAccount() != null ? e.getIrpAccount().getIrpAcctNo() : null)
			    	            .dcAccountNo(e.getDcAccount() != null ? e.getDcAccount().getAccountNo() : null)
			    	            .principalId(e.getPrincipal() != null ? e.getPrincipal().getProductId() : null)

			    	            .contractAmount(e.getContractAmount())
			    	            .interestRate(e.getInterestRate())
			    	            .startDate(e.getStartDate())
			    	            .maturityDate(e.getMaturityDate())
			    	            .interestAccrued(e.getInterestAccrued())
			    	            .status(e.getStatus())
			    	            .lastUpdated(e.getLastUpdated())

			    	            .build();
			    	    })
			    	    .toList();
			    
			    return new ResponseAccountHoldingsDto(fundHoldingsDtoList, principalLedgerDtoList);
			} else {
			    return null;
			}
		} catch (JsonProcessingException | IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		}
	}
}
