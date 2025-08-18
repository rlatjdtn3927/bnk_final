// src/main/java/com/example/memo/purchase/handler/SummaryHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.SummaryService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** SUMMARY_* : 계정/계좌의 요약 카드(총평가/수익률/입금합/당일입금/운용수익) */
@Component
@RequiredArgsConstructor
public class SummaryHandler implements TcpMessageHandler {

    private final SummaryService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("SUMMARY_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case SUMMARY_GET:
                return service.getSummary(data);
            default:
                return "알 수 없는 SUMMARY 명령: " + command.name();
        }
    }
}
