// src/main/java/com/example/memo/purchase/handler/MaturityReservationHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.MaturityReservationService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** MATURITY_RESERVATION_* : 만기예정 변경예약 */
@Component
@RequiredArgsConstructor
public class MaturityReservationHandler implements TcpMessageHandler {

    private final MaturityReservationService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("MATURITY_RESERVATION_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case MATURITY_RESERVATION_LIST:
                return service.list(data);
            case MATURITY_RESERVATION_APPLY:
                return service.apply(data);
            default:
                return "알 수 없는 MATURITY_RESERVATION 명령: " + command.name();
        }
    }
}
