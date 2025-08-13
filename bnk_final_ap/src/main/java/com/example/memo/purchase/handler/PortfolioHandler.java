package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.PortfolioService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PortfolioHandler implements TcpMessageHandler {

    private final ObjectMapper om;
    private final PortfolioService service;

    @Override
    public boolean supports(Command c) {
        switch (c) {
            case PORTFOLIO_LIST:
            case PORTFOLIO_CHANGE_PREVIEW:
            case PORTFOLIO_CHANGE_APPLY:
            case MATURITY_RESERVATION_LIST:
            case MATURITY_RESERVATION_APPLY:
            case PENDING_BUY_LIST:
            case PENDING_BUY_UPSERT:
            case PENDING_BUY_HISTORY:
            case DO_UPSERT:
            case DO_HISTORY:
                return true;
            default:
                return false;
        }
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case PORTFOLIO_LIST:
                return service.list(data);
            case PORTFOLIO_CHANGE_PREVIEW:
                return service.changePreview(data);
            case PORTFOLIO_CHANGE_APPLY:
                return service.changeApply(data);
            default:
                ObjectNode err = om.createObjectNode();
                err.put("error", "Not implemented: " + command);
                return err;
        }
    }
}
