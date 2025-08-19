// src/main/java/com/example/memo/purchase/handler/DepositHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.DepositService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** DEPOSIT_* : 계좌 입금/입금내역 */
@Component
@RequiredArgsConstructor
public class DepositHandler implements TcpMessageHandler {

    private final DepositService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("DEPOSIT_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case DEPOSIT_LIST:
                return service.getDeposits(data);
            case DEPOSIT_CREATE:
                return service.createDeposit(data);
            default:
                return "알 수 없는 DEPOSIT 명령: " + command.name();
        }
    }
}
