package com.example.memo.batch;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class batchService {
    @Scheduled(cron = "0 0 1 * * *")	
    public void runDailyJob() {
        
    }
}
