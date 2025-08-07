package com.example.memo.jpa.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.openai.api-key}") // ⭐ API 키 이름 'api-key'로 수정
    private String openAiApiKey;

    @Bean
    public OpenAiApi openAiApi() { // ⭐ OpenAiApi 빈을 수동으로 등록
        return new OpenAiApi(openAiApiKey);
    }
    
    @Bean
    @Primary
    public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
        return new OpenAiEmbeddingModel(openAiApi);
    }

    @Bean
    public PgVectorStore pgVectorStore(@Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate,
                                     EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(pgJdbcTemplate, embeddingModel)
                .vectorTableName("embedding")
                .dimensions(1536)
                .build();
    }
}