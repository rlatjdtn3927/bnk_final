package com.example.memo.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey; // ✅ String으로 변경

    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .apiKey(openAiApiKey)
                .build();
    }
    
    //임베딩 
    @Bean
    public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
        return new OpenAiEmbeddingModel(openAiApi);  // ✅ OpenAI 임베딩 모델 생성
    }

    @Bean
    public PgVectorStore pgVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("embedding")  // ✅ pgvector 테이블 이름
                .dimensions(1536)              // ✅ OpenAI 임베딩 차원
                .build();
    }
}
