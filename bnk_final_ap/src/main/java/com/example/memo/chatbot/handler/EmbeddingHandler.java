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
        return command == Command.EMBEDDING_CREATE;
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            if (command == Command.EMBEDDING_CREATE) {
                return embeddingService.createEmbeddingsFromAllPdfs();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "임베딩 생성 중 오류 발생: " + e.getMessage();
        }
        return "알 수 없는 임베딩 명령: " + command.name();
    }
}