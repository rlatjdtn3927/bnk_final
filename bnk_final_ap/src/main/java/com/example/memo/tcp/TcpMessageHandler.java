package com.example.memo.tcp;

import com.fasterxml.jackson.databind.JsonNode;

public interface TcpMessageHandler {
    Command supports();                 // 자신이 처리할 Command
    JsonNode handle(Message msg) throws Exception;  // 비즈니스 로직 실행
}