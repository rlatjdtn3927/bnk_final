package com.example.memo.irp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.irp.BankAccount;
import com.example.memo.jpa.entity.ledger.FundHoldings;
import com.example.memo.jpa.entity.ledger.FundLedger;
import com.example.memo.jpa.entity.ledger.PrincipalLedger;
import com.example.memo.jpa.entity.purchase.analysis.FundNav;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanFund;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanPG;
import com.example.memo.jpa.repository.irp.BankAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankAccountService {
	
	private final BankAccountRepository bankRepository;
	private final ObjectMapper mapper;
	
	@Transactional(readOnly = true)
	public BankAccount getUserAccounts(Long userId){
		return bankRepository.findByUserId(userId).orElseThrow();
	}
	/*
	@Transactional(readOnly = true)
    public BigDecimal getAccountBalance(String acctNo) {
        BankAccount acct = bankRepository.findById(acctNo).orElseThrow();
        return acct.getBalance();
    }
	*/
	// 보안 검증 + 잔액 조회 (엔티티로)
    @Transactional(readOnly = true)
    public BigDecimal getBalanceForUser(String acctNo, Long userId) {
        return bankRepository.findByAcctNoAndUserId(acctNo, userId)
                .map(BankAccount::getBalance)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
    }
	
	/** 출금계좌 비밀번호 검증: 계좌가 로그인(또는 join의) 사용자 소유인지 + 비번 일치 확인 */
    @Transactional(readOnly = true)
    public boolean verifyAccountPassword(String acctNo, Long userId, String rawPwd) {
        return bankRepository.findByAcctNoAndUserId(acctNo, userId)
                .map(a -> Objects.equals(a.getAcctPwd(), rawPwd))
                .orElse(false);
    }
    
    /** 검증 실패 시 예외 던지는 버전(서비스 내부에서 쓰기 편함) */
    @Transactional(readOnly = true)
    public void verifyOrThrow(String acctNo, Long userId, String rawPwd) {
        if (!verifyAccountPassword(acctNo, userId, rawPwd)) {
            throw new IllegalArgumentException("출금계좌 비밀번호가 일치하지 않습니다.");
        }
    }
    
//    @Transactional
//    public String depositAccount(JsonNode data) {
//        /**매수 예정 등록을 확인해보고 있으면 예정된 품목 구매 아니면 그냥 계좌로 입금**/
//        List<BuyPlanFund> buyPlanList = buyPlanFundRepository.findByDcAccountAndIsCurrent(dest, "Y");
//        if(!buyPlanList.isEmpty()) { //매수 예정 등록분이 있다면 거래 진행
//        	for(BuyPlanFund plan : buyPlanList) {
//        		
//        		Integer ratio = plan.getAllocationPercent();
//        		BigDecimal tradeAmt = temp.multiply(BigDecimal.valueOf(ratio)).divide(BigDecimal.valueOf(100), SCALE_CAL, RMDN);
//        		
//        		FundMaster fund = plan.getFund(); 
//        		String prodId = fund.getProductId();
//        		FundNav fundNav = fundNavRepository.findTopByFund_ProductIdOrderByReferenceDateDesc(prodId).orElseThrow();
//        		BigDecimal nav = fundNav.getNav();
//        		BigDecimal addUnits = tradeAmt.divide(nav, SCALE_CAL, RMDN);
//        		
//        		deposited = deposited.subtract(tradeAmt);
//        		
//        		fundLedgerRepository.save(FundLedger.builder()
//        				.dcAccount(dest)
//        				.fund(fund)
//        				.tradeType("BUY")
//        				.tradeUnits(addUnits.setScale(SCALE_SAVE, RMDN))
//        				.tradePrice(nav.setScale(SCALE_SAVE, RMDN))
//        				.tradeAmount(tradeAmt.setScale(SCALE_SAVE, RMDN))
//        				.build());
//        		
//        		FundHoldings fundholdings = fundHoldingsRepository.findByDcAccountAndFund(dest, fund);
//        		if(fundholdings != null) { //있으면 업데이트 진행
//        			BigDecimal oldUnits = fundholdings.getUnits();
//        			BigDecimal newUnits = oldUnits.add(addUnits); // 새로운 보유좌수
//        			BigDecimal acquisitionAmount = fundholdings.getAcquisitionAmount();
//        			BigDecimal newAvgPrice = acquisitionAmount.add(tradeAmt).divide(newUnits, SCALE_CAL, RMDN); // 새로운 평균단가
//        			BigDecimal newAcquisitionAmount = newAvgPrice.multiply(newUnits); //새로운 매수원금
//        			BigDecimal newValuationAmt = newUnits.multiply(nav); //새로운 평가액
//        			BigDecimal newProfitLoss = newValuationAmt.subtract(newAcquisitionAmount); //새로운 평가손익
//		    		BigDecimal newRr = newAcquisitionAmount.signum()==0 ? BigDecimal.ZERO : // 새로운 수익률 산출
//		    		    newProfitLoss.multiply(BigDecimal.valueOf(100))
//		    		                 .divide(newAcquisitionAmount, SCALE_RATE, RMUP);
//		    		
//		    		fundholdings.setUnits(newUnits.setScale(SCALE_SAVE, RMDN));
//		    		fundholdings.setAvgPrice(newAvgPrice.setScale(SCALE_SAVE, RMDN));
//		    		fundholdings.setAcquisitionAmount(newAcquisitionAmount.setScale(SCALE_SAVE, RMDN));
//		    		fundholdings.setValuationAmount(newValuationAmt.setScale(SCALE_SAVE, RMDN));
//		    		fundholdings.setProfitLoss(newProfitLoss.setScale(SCALE_SAVE, RMDN));
//		    		fundholdings.setReturnRate(newRr.setScale(SCALE_RATE, RMUP));
//		    		
//		    		fundHoldingsRepository.save(fundholdings);
//        		} else { //없으면 새로 삽입
//        			fundHoldingsRepository.save(FundHoldings.builder()
//        					.units(addUnits.setScale(SCALE_SAVE, RMDN))
//        					.dcAccount(dest)
//        					.fund(fund)
//        					.avgPrice(nav)
//        					.acquisitionAmount(tradeAmt)
//        					.valuationAmount(tradeAmt)
//        					.profitLoss(BigDecimal.ZERO)
//        					.returnRate(BigDecimal.ZERO)
//        					.build());
//        		}
//        	}
//        }
//        
//		List<BuyPlanPG> buyPlanPgList = buyPlanPGRepository.findByDcAccountAndIsCurrent(dest, "Y");
//		if(buyPlanPgList.isEmpty()) {
//			for(BuyPlanPG buyPlan : buyPlanPgList) {
//				Integer ratio = buyPlan.getAllocationPercent();
//				BigDecimal tradeAmt = temp.multiply(BigDecimal.valueOf(ratio)).divide(BigDecimal.valueOf(100), SCALE_CAL, RMDN);
//				
//				deposited = deposited.subtract(tradeAmt);
//				
//				PrincipalGuarantee pg = buyPlan.getPrincipal();
//				long years = 0L;
//				switch (pg.getMaturityYears()) {
//    				case "1년" -> years = 1L;
//    				case "2년" -> years = 2L;
//    				case "3년" -> years = 3L;
//    				case "5년" -> years = 5L;
//				}
//				principalLedgerRepository.save(PrincipalLedger.builder()
//						.dcAccount(dest)
//						.principal(pg)
//						.contractAmount(tradeAmt.setScale(SCALE_SAVE, RMDN))
//						.interestRate(pg.getDcRate())
//						.startDate(LocalDate.now())
//						.maturityDate(LocalDate.now().plusYears(years))
//						.interestAccrued(BigDecimal.ZERO)
//						.status("ACTIVE")
//						.build());
//			}
//		}
//        	
//        dest.setBalance(dBal.add(deposited));
//
//    } 
}
