// src/main/java/com/example/memo/purchase/handler/TransactionHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.TransactionService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** TXN_* : 거래내역(상품 매수/매도) */
@Component
@RequiredArgsConstructor
public class TransactionHandler implements TcpMessageHandler {

    private final TransactionService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("TXN_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case TXN_LIST:
                return service.getTransactions(data);
            default:
                return "알 수 없는 TXN 명령: " + command.name();
        }
    }
}
