// src/main/java/com/example/memo/purchase/handler/PendingBuyHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.PendingBuyService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** PENDING_BUY_* : 매수예정 등록/변경/내역 */
@Component
@RequiredArgsConstructor
public class PendingBuyHandler implements TcpMessageHandler {

    private final PendingBuyService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("PENDING_BUY_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case PENDING_BUY_LIST:
                return service.list(data);
            case PENDING_BUY_UPSERT:
                return service.upsert(data);
            case PENDING_BUY_HISTORY:
                return service.history(data);
            default:
                return "알 수 없는 PENDING_BUY 명령: " + command.name();
        }
    }
}
