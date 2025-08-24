package com.example.memo.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.ledger.FundHoldings;
import com.example.memo.jpa.entity.ledger.PrincipalHoldings;
import com.example.memo.jpa.entity.purchase.analysis.FundNav;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.repository.ledger.FundHoldingsRepository;
import com.example.memo.jpa.repository.ledger.PrincipalHoldingsRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundNavRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateHoldings {

    
	static final int SCALE_CAL = 12;
	static final int SCALE_SAVE = 6;
	static final int SCALE_RATE = 4;
	static final RoundingMode RMUP = RoundingMode.HALF_UP;
	static final RoundingMode RMDN = RoundingMode.DOWN;
	
    private final FundHoldingsRepository fundHoldingsRepository;
    private final PrincipalHoldingsRepository principalHoldingsRepository;
    private final FundNavRepository fundNavRepository;
    
    @Transactional
    public void updateFundHoldings() {
    	List<FundHoldings> fundHoldingList = fundHoldingsRepository.findAll();
    	
    	if(!fundHoldingList.isEmpty()) {
    		for(FundHoldings holdings : fundHoldingList) {
    			FundMaster fund = holdings.getFund();
    			FundNav fundNav = fundNavRepository.findTopByFund_ProductIdOrderByReferenceDateDesc(fund.getProductId()).orElse(null);
    			if(fundNav == null) continue;
    			BigDecimal nav = fundNav.getNav();
    			
    			BigDecimal newValueAmt = holdings.getUnits().multiply(nav);
    			holdings.setValuationAmount((newValueAmt.setScale(SCALE_SAVE, RMUP)));
    			BigDecimal newProfitLoss = newValueAmt.subtract(holdings.getAcquisitionAmount());
    			holdings.setProfitLoss(newProfitLoss.setScale(SCALE_SAVE, RMUP));
    			BigDecimal newRr = newProfitLoss.divide(holdings.getAcquisitionAmount(), SCALE_CAL, RMUP);
    			holdings.setReturnRate(newRr);
    			
    			fundHoldingsRepository.save(holdings);
    		}
    	}
    }
    
    @Transactional
    public void updatePrincipalHoldings() {
    	List<PrincipalHoldings> principalHoldingList = principalHoldingsRepository.findAll();
    	if(!principalHoldingList.isEmpty()) {
    		for(PrincipalHoldings holdings : principalHoldingList) {
    			BigDecimal contractAmount = holdings.getContractAmount();
    			LocalDate startDate = holdings.getStartDate();
    			long days = ChronoUnit.DAYS.between(startDate, LocalDate.now())%365;
    			
    			BigDecimal interestRate = holdings.getInterestRate();
    			BigDecimal wholeInterest = contractAmount.multiply(interestRate).divide(BigDecimal.valueOf(100) , SCALE_CAL, RMUP);
    			BigDecimal proRatedInterest = wholeInterest.multiply(BigDecimal.valueOf(days));
    					
    		}
    	}
    }
}
