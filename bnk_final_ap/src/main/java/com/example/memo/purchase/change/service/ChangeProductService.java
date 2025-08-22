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
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.jpa.repository.ledger.FundHoldingsRepository;
import com.example.memo.jpa.repository.ledger.FundLedgerRepository;
import com.example.memo.jpa.repository.ledger.PrincipalLedgerRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundNavRepository;
import com.example.memo.purchase.change.dto.request.BuyFundDto;
import com.example.memo.purchase.change.dto.request.BuyPrincipalDto;
import com.example.memo.purchase.change.dto.request.RequestChangeDto;
import com.example.memo.purchase.change.dto.request.RequestRetainHoldingsDto;
import com.example.memo.purchase.change.dto.request.SoldFundDto;
import com.example.memo.purchase.change.dto.request.SoldPrincipalDto;
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
	
	static final int SCALE_CAL = 12;
	static final int SCALE_SAVE = 6;
	static final int SCALE_RATE = 4;
	static final RoundingMode RMUP = RoundingMode.HALF_UP;
	static final RoundingMode RMDN = RoundingMode.HALF_DOWN;
	
	private final FundLedgerRepository fundLedgerRepository;
	private final FundHoldingsRepository fundHoldingsRepository;
	private final PrincipalLedgerRepository principalLedgerRepository;
	private final FundNavRepository fundNavRepository;
	private final IrpAccountRepository irpAccountRepository;
	private final DcAccountRepository dcAccountRepository;
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
			List<SoldPrincipalDto> soldPrincipalIdList = dto.getSoldPrincipalIdList();
			List<BuyPrincipalDto> buyPrincipalList = dto.getBuyPrincipalList();
			List<FundHoldings> fundHolding = null;
			
			fundHolding = "DC".equals(accountType)
				    ? fundHoldingsRepository.findByDcAccount(DcAccount.builder().accountNo(accountId).build())
				    : fundHoldingsRepository.findByIrpAccount(IrpAccount.builder().irpAcctNo(accountId).build());
				
			if(soldProdList != null) { //거래원장에 매도로 기록 + 집계 테이블 매도 업데이트
				/*****보유현황(집계 테이블) 업데이트 및 삭제 진행******/
				if(fundHolding.isEmpty()) return "해당 계좌에 보유상품 목록이 존재하지 않습니다!";
				
				for(SoldFundDto soldFundDto : soldProdList) {
					String prodId = soldFundDto.getProdId();
					Integer ratio = soldFundDto.getRatio();
					
				    FundHoldings targetEntity = fundHolding.stream()
				    		.filter(f -> f.getFund().getProductId().equals(prodId))
				    		.findFirst()  .orElseThrow(() -> new RuntimeException("Not found"));
				    
				    BigDecimal holdingQty = targetEntity.getUnits();
				    BigDecimal remainRatio = BigDecimal.valueOf(100-ratio)
				    		.divide(BigDecimal.valueOf(100) ,SCALE_CAL, RMUP);
				    
				    BigDecimal newUnits = holdingQty.multiply(remainRatio);
				    FundNav fundNav = fundNavRepository.findTopByFund_ProductIdOrderByReferenceDateDesc(prodId).orElseThrow();
			    	BigDecimal nav = fundNav.getNav();
			    	
			    	/***********거래원장에 거래내역 삽입**************/
				    BigDecimal tradeUnits = holdingQty.multiply(BigDecimal.valueOf(ratio)
							.divide(BigDecimal.valueOf(100), SCALE_CAL, RMDN)); //거래좌수
					BigDecimal tradeAmount = nav.multiply(tradeUnits); //거래금액 : 매도 금액

					FundLedger fundLedger = FundLedger.builder()
							.fund(FundMaster.builder().productId(prodId).build())
							.tradeType("SELL")
							.tradeUnits(tradeUnits.setScale(SCALE_SAVE,RMDN))
							.tradeAmount(tradeAmount.setScale(SCALE_SAVE, RMDN))
							.build();
						
					BigDecimal sellAmount = targetEntity.getAvgPrice().multiply(tradeUnits); //파는 부분만큼의 매수원금
					BigDecimal profit = tradeAmount.subtract(sellAmount); //이익 산출
					
					if("DC".equals(accountType)) {
						DcAccount dcAccount = dcAccountRepository.findByAccountNo(accountId);
						Long oldBalance = dcAccount.getBalance();
						dcAccount.setBalance(oldBalance + profit.longValue());
						dcAccountRepository.save(dcAccount); // 수익 계좌 입금
						fundLedger.setDcAccount(DcAccount.builder().accountNo(accountId).build());
					} 
					else {
						IrpAccount irpAccount = irpAccountRepository.findByIrpAcctNo(accountId).orElse(null);
						BigDecimal oldBalance = irpAccount.getBalance();
						irpAccount .setBalance(oldBalance.add(profit));
						irpAccountRepository.save(irpAccount); // 수익 계좌 입금
						fundLedger.setIrpAccount(IrpAccount.builder().irpAcctNo(accountId).build());
					} 
					fundLedgerRepository.save(fundLedger);
					
					if(newUnits.signum() == 0) {
						fundHoldingsRepository.delete(targetEntity); //보유좌수가 0이면 행 삭제
						fundHolding.removeIf(h -> h.getFund().getProductId()
			                     .equals(targetEntity.getFund().getProductId()));
						continue;
					}
					/***********거래원장에 거래내역 삽입**************/
				    
			    	BigDecimal avgPrice = targetEntity.getAvgPrice();
			    	BigDecimal newAcquisitionAmount = avgPrice.multiply(newUnits);
			    	
			    	BigDecimal newValuationAmount = newUnits.multiply(nav);
			    	BigDecimal newProfitLoss = newValuationAmount.subtract(newAcquisitionAmount);
			    	
			    	BigDecimal newReturn = newProfitLoss.divide(newAcquisitionAmount, SCALE_CAL,RMDN) //새로운 수익률 산출
	    			.multiply(BigDecimal.valueOf(100));
			    	
			    	targetEntity.setUnits(newUnits.setScale(SCALE_SAVE,RMDN)); //새 좌수 업데이트
			    	targetEntity.setValuationAmount(newValuationAmount.setScale(SCALE_SAVE,RMDN)); //새로운 평가액 산출
			    	targetEntity.setAcquisitionAmount(newAcquisitionAmount.setScale(SCALE_SAVE,RMUP)); //새로운 매수원금 산출
			    	targetEntity.setProfitLoss(newProfitLoss.setScale(SCALE_SAVE,RMDN)); //새로운 평가손익 산출
			    	targetEntity.setReturnRate(newReturn.setScale(SCALE_RATE,RMDN)); //새로운 수익률 산출
			    	
			    	fundHoldingsRepository.save(targetEntity);
				}
				/*****보유현황(집계 테이블) 업데이트 및 삭제 진행******/
			}
			if(buyFundList != null) { //거래원장에 매수로 기록 + 집계 테이블 매수 업데이트
				/*****보유현황(집계 테이블) 업데이트 진행******/
				for(BuyFundDto fundDto : buyFundList) {
					String prodId = fundDto.getProdId();
					BigDecimal tradeAmount = fundDto.getCost();
					FundNav fundNav = fundNavRepository.findTopByFund_ProductIdOrderByReferenceDateDesc(prodId).orElseThrow();
			    	BigDecimal nav = fundNav.getNav();
			    	BigDecimal tradeUnits = tradeAmount.divide(nav, SCALE_CAL,RMDN);
			    	
			    	insertFundLedger(prodId, tradeUnits, tradeAmount, accountType, accountId); /**거래 원장에 매수 거래 기록 저장**/
			    	
			    	FundHoldings targetEntity = fundHolding.stream()
					    		.filter(f -> f.getFund().getProductId().equals(prodId))
					    		.findFirst().orElse(null);
			    	
			    	if(targetEntity == null) { /****새로운 집계 데이터 추가****/
			    		insertFundHoldings(tradeUnits, nav, accountType, prodId, accountId);
			    	} else {  /****집계 데이터 업데이트****/
			    		updateFundHoldings(targetEntity, tradeAmount, tradeUnits, nav);
			    	}
				}
			}
			if(soldPrincipalIdList != null) {
				
			}
			if(buyPrincipalList != null) {
				
			}
			return "상품변경 신청이 완료 되었습니다.";
			
		} catch(Exception e) {
			e.printStackTrace();
			return "오류로 인해 상품변경에 실패했습니다.";
		}
		
		
	}
	
	private void insertFundLedger(String prodId , BigDecimal tradeUnits, BigDecimal tradeAmount, String accountType, String accountId) {
		FundLedger fundLedger = FundLedger.builder()
				.fund(FundMaster.builder().productId(prodId).build())
				.tradeType("BUY")
				.tradeUnits(tradeUnits.setScale(SCALE_SAVE, RMDN))
				.tradeAmount(tradeAmount.setScale(SCALE_SAVE, RMDN))
				.build();
		if("DC".equals(accountType)) fundLedger.setDcAccount(DcAccount.builder().accountNo(accountId).build());
		else fundLedger.setIrpAccount(IrpAccount.builder().irpAcctNo(accountId).build());
		fundLedgerRepository.save(fundLedger);
	}
	
	private void updateFundHoldings(FundHoldings targetEntity, BigDecimal tradeAmount, BigDecimal tradeUnits, BigDecimal nav) {
		BigDecimal oldUnits = targetEntity.getUnits();
		BigDecimal newUnits = oldUnits.add(tradeUnits);
		targetEntity.setUnits(newUnits); // 매수 좌수 추가
		
		BigDecimal acquisitionAmount = targetEntity.getAcquisitionAmount();
		BigDecimal newAvgPrice = acquisitionAmount.add(tradeAmount).divide(newUnits, SCALE_CAL, RMDN);
		//새 평균단가 = (기존 총 원가 + 신규 매수 금액) ÷ (기존 좌수 + 신규 좌수)
		
		
		BigDecimal newAcquisitionAmount = newAvgPrice.multiply(newUnits); //새 매수원금 산출
		BigDecimal newValuationAmount = newUnits.multiply(nav); //새로운 평가액 산출
		BigDecimal newProfitLoss = newValuationAmount.subtract(newAcquisitionAmount); //새로운 평가손익 산출
		BigDecimal rr = newAcquisitionAmount.signum()==0 ? BigDecimal.ZERO : // 새로운 수익률 산출
		    newProfitLoss.multiply(BigDecimal.valueOf(100))
		                 .divide(newAcquisitionAmount, SCALE_RATE, RMDN);
		
		
		targetEntity.setAvgPrice(newAvgPrice.setScale(SCALE_SAVE, RMDN)); // 새로운 평균 단가 추가
		targetEntity.setAcquisitionAmount(newAcquisitionAmount.setScale(SCALE_SAVE, RMDN)); //새 매수원금 추가
		targetEntity.setValuationAmount(newValuationAmount.setScale(SCALE_SAVE, RMDN)); //새로운 평가액 추가
		targetEntity.setProfitLoss(newProfitLoss.setScale(SCALE_SAVE, RMDN)); //새로운 평가손익 추가
		targetEntity.setReturnRate(rr); //새로운 수익률 추가
		
		fundHoldingsRepository.save(targetEntity);
		
	}
	
	private void insertFundHoldings(BigDecimal tradeUnits, BigDecimal nav, String accountType, String prodId, String accountId) {
		BigDecimal initailVal = tradeUnits.multiply(nav);
		FundHoldings newEntity = FundHoldings.builder()
				.fund(FundMaster.builder().productId(prodId).build())
				.units(tradeUnits)
				.avgPrice(nav)
				.acquisitionAmount(initailVal.setScale(SCALE_SAVE, RMDN))
				.valuationAmount(initailVal.setScale(SCALE_SAVE, RMDN))
				.profitLoss(BigDecimal.ZERO)
				.returnRate(BigDecimal.ZERO)
				.build();
		if("DC".equals(accountType)) newEntity.setDcAccount(DcAccount.builder().accountNo(accountId).build());
		else newEntity.setIrpAccount(IrpAccount.builder().irpAcctNo(accountId).build());
		fundHoldingsRepository.save(newEntity);
	}
	
	
}
