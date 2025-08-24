package com.example.memo.purchase.change.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import com.example.memo.admin.service.FileDownloadService;
import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.ledger.FundHoldings;
import com.example.memo.jpa.entity.ledger.FundLedger;
import com.example.memo.jpa.entity.ledger.PrincipalHoldings;
import com.example.memo.jpa.entity.purchase.analysis.FundNav;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.jpa.repository.ledger.FundHoldingsRepository;
import com.example.memo.jpa.repository.ledger.FundLedgerRepository;
import com.example.memo.jpa.repository.ledger.PrincipalHoldingsRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundNavRepository;
import com.example.memo.jpa.repository.purchase.commodity.PrincipalGuaranteeRepository;
import com.example.memo.purchase.change.dto.request.BuyFundDto;
import com.example.memo.purchase.change.dto.request.BuyPrincipalDto;
import com.example.memo.purchase.change.dto.request.RequestChangeDto;
import com.example.memo.purchase.change.dto.request.RequestRetainHoldingsDto;
import com.example.memo.purchase.change.dto.request.SoldFundDto;
import com.example.memo.purchase.change.dto.request.SoldPrincipalDto;
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
	
	static final int SCALE_CAL = 12;
	static final int SCALE_SAVE = 6;
	static final int SCALE_RATE = 4;
	static final RoundingMode RMUP = RoundingMode.HALF_UP;
	static final RoundingMode RMDN = RoundingMode.DOWN;
	
	private final FundLedgerRepository fundLedgerRepository;
	private final FundHoldingsRepository fundHoldingsRepository;
	private final PrincipalHoldingsRepository principalHoldingsRepository;
	private final FundNavRepository fundNavRepository;
	private final IrpAccountRepository irpAccountRepository;
	private final DcAccountRepository dcAccountRepository;
	private final PrincipalGuaranteeRepository principalGuaranteeRepository;
	private final ObjectMapper mapper;

	
	@Transactional
	public ResponseAccountHoldingsDto getHoldings(JsonNode data) {

		try {
			RequestRetainHoldingsDto dto = mapper.treeToValue(data, RequestRetainHoldingsDto.class);
			String accountType = dto.getAccountType();
			String accountId = dto.getAccountId();
			List<FundHoldings> fundHoldingsList = null;
			List<PrincipalHoldings> principalHoldingList = null;
			if("DC".equals(accountType)) {
				fundHoldingsList = fundHoldingsRepository.findByDcAccount_AccountNo(accountId);
				principalHoldingList = principalHoldingsRepository.findByDcAccount_AccountNo(accountId);
			} else { //irp
				IrpAccount proxy = IrpAccount.builder().irpAcctNo(accountId).build();
				fundHoldingsList = fundHoldingsRepository.findByIrpAccount(proxy);
				principalHoldingList = principalHoldingsRepository.findByIrpAccount(proxy);
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
			    	    .map((PrincipalHoldings e) -> {
			    	        return PrincipalLedgerDto.builder()
			    	            .id(e.getId())
			    	            .irpAccountId(e.getIrpAccount() != null ? e.getIrpAccount().getIrpAcctNo() : null)
			    	            .dcAccountNo(e.getDcAccount() != null ? e.getDcAccount().getAccountNo() : null)
			    	            .principalId(e.getPrincipal() != null ? e.getPrincipal().getProductId() : null)
			    	            .prodName(e.getPrincipal().getBankName() + " " + e.getPrincipal().getProductName() + " " + e.getPrincipal().getMaturityYears())
			    	            .contractAmount(e.getContractAmount())
			    	            .interestRate(e.getInterestRate())
			    	            .startDate(e.getStartDate())
			    	            .maturityDate(e.getMaturityDate())
			    	            .interestAccrued(e.getInterestAccrued())
			    	            .status(e.getStatus())

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
			
			List<FundHoldings>  fundHolding = "DC".equals(accountType)
				    ? fundHoldingsRepository.findByDcAccount_AccountNo(accountId)
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
				    
				    BigDecimal holdingQty = targetEntity.getUnits(); //현재 남은 좌수
				    BigDecimal soldUnits = holdingQty //판매 좌수
				    	    .multiply(BigDecimal.valueOf(ratio))
				    	    .divide(BigDecimal.valueOf(100), SCALE_CAL, RMDN);
				    BigDecimal newUnits = holdingQty.subtract(soldUnits); //남은 좌수
				    
				    FundNav fundNav = fundNavRepository.findTopByFund_ProductIdOrderByReferenceDateDesc(prodId).orElseThrow();
			    	BigDecimal nav = fundNav.getNav(); //기준가
			    	
			    	/***********거래원장에 거래내역 삽입**************/
				    
					BigDecimal tradeAmount = nav.multiply(soldUnits); //거래금액 : 매도 금액

					FundLedger fundLedger = FundLedger.builder()
							.fund(FundMaster.builder().productId(prodId).build())
							.tradeType("SELL")
							.tradeUnits(soldUnits.setScale(SCALE_SAVE,RMDN))
							.tradeAmount(tradeAmount.setScale(SCALE_SAVE, RMDN))
							.build();
						
					BigDecimal sellAmount = targetEntity.getAvgPrice().multiply(soldUnits); //파는 부분만큼의 매수원금
					BigDecimal profit = tradeAmount.subtract(sellAmount); //이익 산출
					
					if("DC".equals(accountType)) {
						DcAccount dcAccount = dcAccountRepository.findByAccountNo(accountId);
						BigDecimal oldBalance = dcAccount.getBalance();
						dcAccount.setBalance(oldBalance.add(profit).setScale(SCALE_SAVE, RMUP));
						dcAccountRepository.save(dcAccount); // 수익 계좌 입금
						fundLedger.setDcAccount(DcAccount.builder().accountNo(accountId).build());
					} 
					else {
						IrpAccount irpAccount = irpAccountRepository.findByIrpAcctNo(accountId).orElse(null);
						BigDecimal oldBalance = irpAccount.getBalance();
						irpAccount .setBalance(oldBalance.add(profit).setScale(SCALE_SAVE, RMUP));
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
			    	BigDecimal newReturn = (newAcquisitionAmount.signum()==0)
			    		    ? BigDecimal.ZERO
			    		    : newProfitLoss.multiply(BigDecimal.valueOf(100))
			    		                   .divide(newAcquisitionAmount, SCALE_RATE, RoundingMode.HALF_UP);
			    	
			    	targetEntity.setUnits(newUnits.setScale(SCALE_SAVE,RMDN)); //새 좌수 업데이트
			    	targetEntity.setValuationAmount(newValuationAmount.setScale(SCALE_SAVE,RMDN)); //새로운 평가액 산출
			    	targetEntity.setAcquisitionAmount(newAcquisitionAmount.setScale(SCALE_SAVE,RMUP)); //새로운 매수원금 산출
			    	targetEntity.setProfitLoss(newProfitLoss.setScale(SCALE_SAVE,RMDN)); //새로운 평가손익 산출
			    	targetEntity.setReturnRate(newReturn.setScale(SCALE_RATE,RMUP)); //새로운 수익률 산출
			    	
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
			    	
			    	/**거래 원장에 매수 거래 기록 저장**/
					FundLedger fundLedger = FundLedger.builder()
							.fund(FundMaster.builder().productId(prodId).build())
							.tradeType("BUY")
							.tradeUnits(tradeUnits.setScale(SCALE_SAVE, RMDN))
							.tradeAmount(tradeAmount.setScale(SCALE_SAVE, RMDN))
							.build();
					if("DC".equals(accountType)) fundLedger.setDcAccount(DcAccount.builder().accountNo(accountId).build());
					else fundLedger.setIrpAccount(IrpAccount.builder().irpAcctNo(accountId).build());
					fundLedgerRepository.save(fundLedger);
					/**거래 원장에 매수 거래 기록 저장**/
			    	
			    	FundHoldings targetEntity = fundHolding.stream()
					    		.filter(f -> f.getFund().getProductId().equals(prodId))
					    		.findFirst().orElse(null);
			    	
			    	if(targetEntity == null) { //새로운 집계 데이터 추가
			    		/****새로운 집계 데이터 추가****/
			    		
			    		BigDecimal initailVal = tradeUnits.multiply(nav);
			    		FundHoldings newEntity = FundHoldings.builder()
			    				.fund(FundMaster.builder().productId(prodId).build())
			    				.units(tradeUnits.setScale(SCALE_SAVE, RMDN))
			    				.avgPrice(nav.setScale(SCALE_SAVE, RMDN))
			    				.acquisitionAmount(initailVal.setScale(SCALE_SAVE, RMDN))
			    				.valuationAmount(initailVal.setScale(SCALE_SAVE, RMDN))
			    				.profitLoss(BigDecimal.ZERO)
			    				.returnRate(BigDecimal.ZERO)
			    				.build();
						if("DC".equals(accountType)) newEntity.setDcAccount(DcAccount.builder().accountNo(accountId).build());
						else newEntity.setIrpAccount(IrpAccount.builder().irpAcctNo(accountId).build());
						fundHoldingsRepository.save(newEntity);
						
						/****새로운 집계 데이터 추가****/
			    	} else { //있는 상품 목록 업데이트
			    		BigDecimal oldUnits = targetEntity.getUnits();
			    		BigDecimal newUnits = oldUnits.add(tradeUnits);
			    		
			    		BigDecimal acquisitionAmount = targetEntity.getAcquisitionAmount();
			    		BigDecimal newAvgPrice = acquisitionAmount.add(tradeAmount).divide(newUnits, SCALE_CAL, RMUP);
			    		//새 평균단가 = (기존 총 원가 + 신규 매수 금액) ÷ (기존 좌수 + 신규 좌수)
			    		
			    		
			    		BigDecimal newAcquisitionAmount = newAvgPrice.multiply(newUnits); //새 매수원금 산출
			    		BigDecimal newValuationAmount = newUnits.multiply(nav); //새로운 평가액 산출
			    		BigDecimal newProfitLoss = newValuationAmount.subtract(newAcquisitionAmount); //새로운 평가손익 산출
			    		BigDecimal rr = newAcquisitionAmount.signum()==0 ? BigDecimal.ZERO : // 새로운 수익률 산출
			    		    newProfitLoss.multiply(BigDecimal.valueOf(100))
			    		                 .divide(newAcquisitionAmount, SCALE_RATE, RMUP);
			    		
			    		
			    		targetEntity.setUnits(newUnits.setScale(SCALE_SAVE, RMUP)); // 매수 좌수 추가
			    		targetEntity.setAvgPrice(newAvgPrice.setScale(SCALE_SAVE, RMDN)); // 새로운 평균 단가 추가
			    		targetEntity.setAcquisitionAmount(newAcquisitionAmount.setScale(SCALE_SAVE, RMDN)); //새 매수원금 추가
			    		targetEntity.setValuationAmount(newValuationAmount.setScale(SCALE_SAVE, RMDN)); //새로운 평가액 추가
			    		targetEntity.setProfitLoss(newProfitLoss.setScale(SCALE_SAVE, RMDN)); //새로운 평가손익 추가
			    		targetEntity.setReturnRate(rr); //새로운 수익률 추가
			    		
			    		fundHoldingsRepository.save(targetEntity);
			    	}
				}
			}
			
			List<PrincipalHoldings> principalHolding = "DC".equals(accountType)
				    ? principalHoldingsRepository.findByDcAccount_AccountNo(accountId)
				    : principalHoldingsRepository.findByIrpAccount(IrpAccount.builder().irpAcctNo(accountId).build());
			
			if(soldPrincipalIdList != null) { //원리금 보장 상품 매도의 경우
				
			    for(SoldPrincipalDto soldDto : soldPrincipalIdList) {
			    	Long prodId = soldDto.getId();
			    	Integer ratio = soldDto.getRatio();
			        PrincipalHoldings targetEntity = principalHolding.stream()
			    			.filter(f -> f.getId().equals(prodId))
			    			.findFirst().orElse(null);
			        if(targetEntity == null) return "해당 계좌에 보유상품 목록이 존재하지 않습니다!";
			        
			        BigDecimal contractAmount = targetEntity.getContractAmount();
			        BigDecimal tradeAmount = contractAmount.multiply(BigDecimal.valueOf(ratio))
			        		.divide(BigDecimal.valueOf(100), SCALE_CAL, RMDN);
			        BigDecimal remainAmount = contractAmount.subtract(tradeAmount); // 매도 후 남은 원금
			        
			        targetEntity.setContractAmount(remainAmount.setScale(SCALE_SAVE, RMUP));
			        
			        if(remainAmount.signum() == 0) {
			        	targetEntity.setStatus("TERMINATED");
			        	principalHoldingsRepository.save(targetEntity);
			        	continue;
			        }
			        
			        LocalDate startDate = targetEntity.getStartDate();
			        LocalDate today = LocalDate.now();
			        
			        BigDecimal daysBetween = new BigDecimal(ChronoUnit.DAYS.between(startDate, today));
			        BigDecimal interestRate = targetEntity.getInterestRate();
			        BigDecimal proRatedIr = daysBetween.divide(BigDecimal.valueOf(365), SCALE_CAL, RMUP).divide(BigDecimal.valueOf(100), SCALE_CAL, RMUP ).multiply(interestRate);
			        BigDecimal interestAccrued = tradeAmount.multiply(proRatedIr).divide(BigDecimal.valueOf(ratio), SCALE_CAL, RMUP);
			        targetEntity.setInterestAccrued(targetEntity.getInterestAccrued().subtract(interestAccrued).setScale(SCALE_SAVE,RMUP));
			        // 중간 일할 이자율 계산 원래는 해지 이자율로 계산해야 되나 간단히 구현
			        
			        if("DC".equals(accountType)) {
			        	DcAccount dcAccount = dcAccountRepository.findByAccountNo(accountId);
			        	BigDecimal balance = dcAccount.getBalance();
			        	BigDecimal newBalance = balance.add(interestAccrued.setScale(SCALE_SAVE, RMUP));
			        	dcAccount.setBalance(newBalance);
			        	dcAccountRepository.save(dcAccount); //일할 이자율 정산
			        } else {
			        	IrpAccount irpAccount = irpAccountRepository.findByIrpAcctNo(accountId).orElse(null);
			        	BigDecimal balance = irpAccount.getBalance();
			        	BigDecimal newBalance = balance.add(interestAccrued).setScale(SCALE_SAVE, RMUP);
			        	irpAccount.setBalance(newBalance);
			        	irpAccountRepository.save(irpAccount); //일할 이자율 정산
			        }
			        
			        principalHoldingsRepository.save(targetEntity);
			    }
			}
			if(buyPrincipalList != null) { //원리금 보장 상품 매수의 경우
				
				for(BuyPrincipalDto buyDto : buyPrincipalList) {
					String prodId = buyDto.getProdId();
					BigDecimal tradeAmt = buyDto.getCost();
					PrincipalGuarantee pg = principalGuaranteeRepository.findByProductId(prodId).orElse(null);

					long years;
					switch (pg.getMaturityYears()) {
					  case "1년" -> years = 1L;
					  case "2년" -> years = 2L;
					  case "3년" -> years = 3L;
					  case "5년" -> years = 5L;
					  default -> throw new IllegalArgumentException("지원하지 않는 만기");
					}
					
					PrincipalHoldings pgLedger = PrincipalHoldings.builder()
							.principal(pg)
							.contractAmount(tradeAmt.setScale(SCALE_SAVE, RMUP))
							.startDate(LocalDate.now())
							.maturityDate(LocalDate.now().plusYears(years))
							.interestAccrued(BigDecimal.ZERO)
							.status("ACTIVE")
							.build();
					
					if("DC".equals(accountType)) {
						DcAccount dcAccount = dcAccountRepository.findByAccountNo(accountId);
						pgLedger.setDcAccount(dcAccount);
						pgLedger.setInterestRate(pg.getDbRate());
					} else {
						IrpAccount irpAccount = irpAccountRepository.findByIrpAcctNo(accountId).orElse(null);
						pgLedger.setIrpAccount(irpAccount);
						pgLedger.setInterestRate(pg.getIrpRate());
					}
					principalHoldingsRepository.save(pgLedger);
				}
			}
			return "상품변경 신청이 완료 되었습니다.";
			
		} catch(Exception e) {
			e.printStackTrace();
			return "오류로 인해 상품변경에 실패했습니다.";
		}
		
		
	}	
}
