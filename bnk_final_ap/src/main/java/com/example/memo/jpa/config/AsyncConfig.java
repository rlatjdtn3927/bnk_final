// src/main/java/com/example/memo/jpa/config/AsyncConfig.java
package com.example.memo.jpa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
	//파일 단위 비동기 처리용 스레드폴
	//스레드 폴 생성
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        //기본 직원 수
        ex.setCorePoolSize(6);
        //최대 직원 수
        ex.setMaxPoolSize(12);
        //직원이 다 바쁠 때 기다릴 수 있는 작업 수
        ex.setQueueCapacity(1000);
        ex.setThreadNamePrefix("PdfProcess-");
        ex.initialize();
        return ex;
    }
    //PDF파일이 3000개 있다
    //비동기 X->한 명의 직원이 파일을 하나씩 순서대로 처리
    //비동기+스레드풀->여러 명의 직원이 동시에 여러 파일을 나눠 처리
    //AsyncConfig 안의 taskExecutor가 바로 직원 여러명을 고용하는 설정
}
