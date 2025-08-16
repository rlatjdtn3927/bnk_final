// src/main/java/com/example/memo/purchase/handler/MaturityReservationHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.MaturityReservationService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/**
 * FLOW:
 * - MATURITY_RESERVATION_LIST  : [만기예정 목록 조회] 예금(PRINCIPAL) 보유분을 월/상품 단위로 내려줌
 * - MATURITY_RESERVATION_APPLY : [만기예정 변경예약 적용] 전환일까지 일할이자 계산 → 전환일에 예금 SELL → 목표배분 BUY
 */
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
                // [만기예정 목록 조회]
                return service.listMonthlyPrincipalContributions(data);

            case MATURITY_RESERVATION_APPLY:
                // [만기예정 변경예약 적용]
                return service.applyReservation(data);

            default:
                return "알 수 없는 MATURITY_RESERVATION 명령: " + command.name();
        }
    }
}
