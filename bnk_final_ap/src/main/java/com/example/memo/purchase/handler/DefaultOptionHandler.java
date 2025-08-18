// src/main/java/com/example/memo/purchase/handler/DefaultOptionHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.DefaultOptionService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** DO_* : 디폴트옵션 등록/변경/이력 */
@Component
@RequiredArgsConstructor
public class DefaultOptionHandler implements TcpMessageHandler {

    private final DefaultOptionService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("DO_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case DO_UPSERT:
                return service.upsert(data);
            case DO_HISTORY:
                return service.history(data);
            default:
                return "알 수 없는 DO 명령: " + command.name();
        }
    }
}
