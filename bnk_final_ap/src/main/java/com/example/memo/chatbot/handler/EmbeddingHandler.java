package com.example.memo.chatbot.handler;

import org.springframework.stereotype.Component;

import com.example.memo.chatbot.service.EmbeddingService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmbeddingHandler implements TcpMessageHandler {

    private final EmbeddingService embeddingService;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("EMBEDDING_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            switch (command) {
                case EMBEDDING_CREATE:
                    return embeddingService.createEmbeddingsFromAllPdfs();
                // 향후 추가 가능:
                // case EMBEDDING_DELETE:
                //     return embeddingService.deleteEmbedding(data.get("id").asText());
                default:
                    return "알 수 없는 EMBEDDING 명령: " + command.name();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "임베딩 처리 중 오류 발생: " + e.getMessage();
        }
    }
}
