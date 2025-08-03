package com.example.memo.tcp_common;

import com.fasterxml.jackson.databind.JsonNode;

public interface TcpMessageHandler {
    boolean supports(Command command);
    String handle(Command command, JsonNode data);
}
