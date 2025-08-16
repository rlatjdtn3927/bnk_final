package com.example.memo.admin.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.*;

@Configuration
public class DownloadInfraConfig {

    @Bean(destroyMethod = "shutdown") // 컨텍스트 종료 시 풀 정리
    public ExecutorService downloadExecutor() {
        int cores = Math.max(2, Runtime.getRuntime().availableProcessors());
        return new ThreadPoolExecutor(
            cores, cores * 3,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000),
            r -> { Thread t = new Thread(r, "download-pool"); t.setDaemon(true); return t; },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
}