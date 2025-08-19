// src/main/java/com/example/memo/purchase/handler/AllocationHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.AllocationService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** ALLOCATION_* : 비율 미리보기/즉시 적용 */
@Component
@RequiredArgsConstructor
public class AllocationHandler implements TcpMessageHandler {

    private final AllocationService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("ALLOCATION_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case ALLOCATION_PREVIEW:
                return service.preview(data);
            case ALLOCATION_APPLY:
                return service.apply(data);
            default:
                return "알 수 없는 ALLOCATION 명령: " + command.name();
        }
    }
}
