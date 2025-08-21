package com.example.memo.purchase.change.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.ledger.FundHoldings;
import com.example.memo.jpa.entity.ledger.FundLedger;
import com.example.memo.jpa.entity.ledger.PrincipalLedger;
import com.example.memo.jpa.entity.purchase.analysis.FundNav;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.repository.ledger.FundHoldingsRepository;
import com.example.memo.jpa.repository.ledger.FundLedgerRepository;
import com.example.memo.jpa.repository.ledger.PrincipalLedgerRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundNavRepository;
import com.example.memo.purchase.change.dto.request.BuyFundDto;
import com.example.memo.purchase.change.dto.request.BuyPrincipalDto;
import com.example.memo.purchase.change.dto.request.RequestChangeDto;
import com.example.memo.purchase.change.dto.request.RequestRetainHoldingsDto;
import com.example.memo.purchase.change.dto.request.SoldFundDto;
import com.example.memo.purchase.change.dto.response.FundHoldingsDto;
import com.example.memo.purchase.change.dto.response.PrincipalLedgerDto;
import com.example.memo.purchase.change.dto.response.ResponseAccountHoldingsDto;
import com.example.memo.purchase.reserve.dto.request.ReserveUpdateRequestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeProductService {
	
	private final FundLedgerRepository fundLedgerRepository;
	private final FundHoldingsRepository fundHoldingsRepository;
	private final PrincipalLedgerRepository principalLedgerRepository;
	private final FundNavRepository fundNavRepository;
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

	@Transactional
	public String updateLedger(JsonNode data) {
		try {
			RequestChangeDto dto = mapper.treeToValue(data,RequestChangeDto.class);
			String accountType = dto.getAccountType();
			String accountId = dto.getAccountId();
			List<SoldFundDto> soldProdList = dto.getSoldProdList();
			List<BuyFundDto> buyFundList = dto.getBuyFundList();
			List<String> soldPrincipalIdList = dto.getSoldPrincipalIdList();
			List<BuyPrincipalDto> buyPrincipalList = dto.getBuyPrincipalList();
			List<FundHoldings> fundHolding = null;
			
			if("DC".equals(accountType)) {
				fundHolding = fundHoldingsRepository.findByDcAccount(DcAccount.builder().accountNo(accountId).build());
			} else { //IRP
				fundHolding = fundHoldingsRepository.findByIrpAccount(IrpAccount.builder().irpAcctNo(accountId).build());
			}
			
			if(fundHolding == null) return "해당 계좌에 보유상품 목록이 존재하지 않습니다!";
			
			if(soldProdList != null) { //거래원장에 매도로 기록 + 집계 테이블에서는 보유좌수를 ratio 곱한 만큼 차감
				/*****보유현황(집계 테이블) 업데이트 및 삭제 진행******/
				for(SoldFundDto soldFundDto : soldProdList) {
					
					String prodId = soldFundDto.getProdId();
					Integer ratio = soldFundDto.getRatio();
					
				    FundHoldings targetEntity = fundHolding.stream()
				    		.filter(f -> f.getFund().getProductId().equals(prodId))
				    		.findFirst()  .orElseThrow(() -> new RuntimeException("Not found"));
				    BigDecimal holdingQty = targetEntity.getUnits();
				    BigDecimal remainRatio = BigDecimal.valueOf(100-ratio)
				    		.divide(BigDecimal.valueOf(100)).setScale(6, RoundingMode.HALF_UP);
				    BigDecimal newUnits = holdingQty.multiply(remainRatio).setScale(6, RoundingMode.HALF_UP);

			    	targetEntity.setUnits(newUnits); //새 좌수 업데이트
			    	BigDecimal avgPrice = targetEntity.getAvgPrice();
			    	BigDecimal newAcquisitionAmount = avgPrice.multiply(newUnits).setScale(6, RoundingMode.HALF_UP);
			    	targetEntity.setAcquisitionAmount(newAcquisitionAmount); //새로운 매수원금 산출
			    	FundNav fundNav = fundNavRepository.findTopByFund_ProductIdOrderByReferenceDateDesc(prodId).orElseThrow();
			    	BigDecimal nav = fundNav.getNav();
			    	BigDecimal newValuationAmount = newUnits.multiply(nav).setScale(6, RoundingMode.HALF_UP);
			    	targetEntity.setValuationAmount(newValuationAmount); //새로운 평가액 산출
			    	BigDecimal newProfitLoss = newValuationAmount.subtract(newAcquisitionAmount).setScale(6, RoundingMode.HALF_UP);
			    	targetEntity.setProfitLoss(newProfitLoss); //새로운 평가손익 산출
			    	targetEntity.setReturnRate(newProfitLoss.divide(newAcquisitionAmount) //새로운 수익률 산출
			    			.multiply(BigDecimal.valueOf(100)).setScale(6, RoundingMode.HALF_UP));
			    	fundHoldingsRepository.save(targetEntity);
			    	
				    if(newUnits.signum() == 0) fundHoldingsRepository.delete(targetEntity); //보유좌수가 0이면 행 삭제
				    
					/***********거래원장에 거래내역 삽입**************/
					BigDecimal tradeUnits = holdingQty.multiply(BigDecimal.valueOf(ratio)
							.divide(BigDecimal.valueOf(100))).setScale(6, RoundingMode.HALF_UP); //거래좌수
					BigDecimal tradeAmount = nav.multiply(tradeUnits).setScale(6, RoundingMode.HALF_UP); //거래금액
					if("DC".equals(accountType)) {
						fundLedgerRepository.save(FundLedger.builder()
								.dcAccount(DcAccount.builder().accountNo(accountId).build())
								.fund(FundMaster.builder().productId(prodId).build())
								.tradeType("SELL")
								.tradeUnits(tradeUnits)
								.tradeAmount(tradeAmount)
								.build());
					} else {
						fundLedgerRepository.save(FundLedger.builder()
								.irpAccount(IrpAccount.builder().irpAcctNo(accountId).build())
								.fund(FundMaster.builder().productId(prodId).build())
								.tradeType("SELL")
								.tradeUnits(tradeUnits)
								.tradeAmount(tradeAmount)
								.build());
					}
				}
				/*****보유현황(집계 테이블) 업데이트 및 삭제 진행******/
			}
			if(buyFundList != null) {
				
			}
			if(soldPrincipalIdList != null) {
				
			}
			if(buyPrincipalList != null) {
				
			}
			return "상품변경 신청이 완료 되었습니다.";
			
		} catch(Exception e) {
			e.printStackTrace();
			return "상품변경 신청이 완료 되었습니다.";
		}
		
		
	}
}
