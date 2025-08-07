package com.example.memo.chatbot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.memo.chatbot.dto.ChatRequestDto;
import com.example.memo.chatbot.dto.EmbeddingRequestDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatbotController {

    private final TcpClientService tcpService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String mainPage() {
        return "index";
    }

    @PostMapping("/api/embeddings")
    @ResponseBody
    public ResponseEntity<?> createEmbeddings(@RequestBody EmbeddingRequestDto requestDto) {
        try {
            JsonNode data = objectMapper.convertValue(requestDto, JsonNode.class);
            TcpMessage msg = new TcpMessage(Command.EMBEDDING_CREATE, data);
            JsonNode response = tcpService.sendMessage(msg);
            return ResponseEntity.status(HttpStatus.OK).body(tcpService.sendMessage(msg));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("임베딩 생성 요청 실패: " + e.getMessage());
        }
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<?> askQuestion(@RequestBody ChatRequestDto requestDto) {
        try {
            JsonNode data = objectMapper.convertValue(requestDto, JsonNode.class);
            TcpMessage msg = new TcpMessage(Command.CHAT_ASK_QUESTION, data);
            JsonNode response = tcpService.sendMessage(msg);
            return ResponseEntity.status(HttpStatus.OK).body(tcpService.sendMessage(msg));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("챗봇 질문 요청 실패: " + e.getMessage());
        }
    }
}
