package com.example.memo.tcp;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//소켓으로 왕복할 공통 메시지
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message {
 private Command cmd;        // 수행할 명령
 private String correlation; // 트래킹용 UUID
 private long timestamp;     // 밀리초
 private JsonNode payload;   // 실데이터
 private String error;       // 실패 시 에러메시지
}