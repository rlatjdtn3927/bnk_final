// src/main/java/com/example/memo/purchase/handler/TradePrereqHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.TradePrereqService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** TRADE_* : 위저드 사전 체크(로그인/IRP 계좌/잔액) */
@Component
@RequiredArgsConstructor
public class TradePrereqHandler implements TcpMessageHandler {

    private final TradePrereqService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("TRADE_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case TRADE_PREREQ_CHECK:
                return service.prereqCheck(data);
            default:
                return "알 수 없는 TRADE 명령: " + command.name();
        }
    }
}
