package com.example.memo.batch;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class batchService {
	
	private final UpdateHoldingsService updateHoldingsService;
	
    @Scheduled(cron = "0 0 1 * * *")	
    public void runDailyJob() {
    	updateHoldingsService.updateFundHoldings();
    	updateHoldingsService.updatePrincipalHoldings();
    }
}
