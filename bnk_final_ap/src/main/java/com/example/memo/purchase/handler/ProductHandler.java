// src/main/java/com/example/memo/purchase/handler/ProductHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.ProductService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** PRODUCT_* : 상품 검색(FUND/ETF/TDF/PRINCIPAL) */
@Component
@RequiredArgsConstructor
public class ProductHandler implements TcpMessageHandler {

    private final ProductService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("PRODUCT_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case PRODUCT_SEARCH:
                return service.searchProducts(data);
            default:
                return "알 수 없는 PRODUCT 명령: " + command.name();
        }
    }
}
