// src/main/java/com/example/memo/purchase/handler/PortfolioHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.PortfolioService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** PORTFOLIO_* : 보유상품 목록/변경(미리보기/적용) */
@Component
@RequiredArgsConstructor
public class PortfolioHandler implements TcpMessageHandler {

    private final PortfolioService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("PORTFOLIO_");
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
                return "알 수 없는 PORTFOLIO 명령: " + command.name();
        }
    }
}
