// AP - src/main/java/com/example/memo/purchase/handler/AccountOverviewHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.AccountOverviewService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountOverviewHandler implements TcpMessageHandler {

    private final AccountOverviewService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("ACCOUNT_OVERVIEW_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case ACCOUNT_OVERVIEW_GET:
                return service.getOverview(data);
            default:
                return "알 수 없는 ACCOUNT_OVERVIEW 명령: " + command.name();
        }
    }
}
