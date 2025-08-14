package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.HoldingsService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HoldingsHandler implements TcpMessageHandler {

    private final ObjectMapper om;
    private final HoldingsService service;

    @Override
    public boolean supports(Command c) {
        return c == Command.SUMMARY_GET ||
               c == Command.TXN_LIST ||
               c == Command.DEPOSIT_LIST ||
               c == Command.DEPOSIT_CREATE;
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case SUMMARY_GET:
                return service.getSummary(data);
            case TXN_LIST:
                return service.getTransactions(data);
            case DEPOSIT_LIST:
                return service.getDeposits(data);
            case DEPOSIT_CREATE:
                return service.createDeposit(data);
            default:
                ObjectNode err = om.createObjectNode();
                err.put("error", "Unknown command: " + command);
                return err;
        }
    }
}
