package com.example.memo.chatbot.handler;

import org.springframework.stereotype.Component;

import com.example.memo.chatbot.service.ChatbotService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatbotHandler implements TcpMessageHandler {

    private final ChatbotService chatbotService;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("CHAT_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            switch (command) {
                case CHAT_ASK_QUESTION:
                    String question = data.get("question").asText();
                    return chatbotService.getChatResponse(question);

                // 추후 명령 추가 시 여기에 케이스 추가
                // case CHAT_CLEAR_SESSION:
                //     return chatbotService.clearSession();

                default:
                    return "알 수 없는 챗봇 명령: " + command.name();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "챗봇 처리 중 오류 발생: " + e.getMessage();
        }
    }
}
