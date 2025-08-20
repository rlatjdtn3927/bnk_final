// Handler (계좌 커맨드)
package com.example.memo.purchase.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.AccountQueryService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PurchaseAccountHandler implements TcpMessageHandler {

    private final AccountQueryService svc;
    
    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("ACCOUNT_GET");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        Long userId = data.get("userId").asLong();
        Map<String, Object> res = new HashMap<>();

        return switch (command) {
            case ACCOUNT_GET_IRP_BY_USER -> {
                res.put("irpAcctNo", svc.getIrpAcctNoByUserId(userId));
                yield res;
            }
            case ACCOUNT_GET_DC_LIST_BY_USER -> {
                res.put("dcAccounts", svc.getDcAccountNosByUserId(userId));
                yield res;
            }
            default -> "Unknown ACCOUNT command: " + command;
        };
    }
}
