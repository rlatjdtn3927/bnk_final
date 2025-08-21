// src/main/java/com/example/memo/purchase/handler/ProductHandler.java
package com.example.memo.purchase.handler;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.dto.commodity.FundMasterDto;
import com.example.memo.purchase.service.ProductService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/**
 * PRODUCT_* : 상품 검색 관련 핸들러
 * - 현재 사용: PRODUCT_SEARCH (카테고리별 검색)
 * - 향후 PRODUCT_DETAIL 등 확장 시 supports/handle 스위치만 추가하면 됨
 */
@Component
@RequiredArgsConstructor
public class ProductHandler implements TcpMessageHandler {

    private final ProductService service;

    /** 'PRODUCT_' 로 시작하는 커맨드 묶음만 수신 */
    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("PRODUCT_");
    }

    /** 커맨드별 분기 */
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
