package com.example.memo.tcp_common;
import com.fasterxml.jackson.databind.JsonNode;


/*이걸 구현한 각자만의 핸들러 만들기(이게 컨트롤러 역할)*/
public interface TcpMessageHandler {
    boolean supports(Command command);
    Object handle(Command command, JsonNode data);
}


