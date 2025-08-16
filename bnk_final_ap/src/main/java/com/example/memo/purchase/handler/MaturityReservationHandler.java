// src/main/java/com/example/memo/purchase/handler/MaturityReservationHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.MaturityReservationService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** MATURITY_RESERVATION_* : 만기예정 변경 흐름(목록/예약) */
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
            case MATURITY_RESERVATION_LIST:   // FLOW: 만기예정 목록(예금 납입분을 '월별'로 묶어 조회)
                return service.listMonthlyPrincipalContributions(data);
            case MATURITY_RESERVATION_APPLY:  // FLOW: 만기 변경 '예약' 저장(※지금은 스텁: OK만 반환)
                return service.applyReservation(data);
            default:
                return "알 수 없는 MATURITY_RESERVATION 명령: " + command.name();
        }
    }
}
