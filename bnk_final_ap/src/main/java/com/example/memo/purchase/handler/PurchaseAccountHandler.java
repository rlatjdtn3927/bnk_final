// Handler (계좌 커맨드)
package com.example.memo.purchase.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.AccountQueryService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PurchaseAccountHandler implements TcpMessageHandler {

	private final AccountQueryService svc;

    @Override
    public boolean supports(Command command) {
        // ✅ 정확 매칭 (다른 핸들러가 ACCOUNT_ 프리픽스 넓게 잡아도 충돌 X)
        return command == Command.ACCOUNT_GET_IRP_BY_USER
            || command == Command.ACCOUNT_GET_DC_LIST_BY_USER;
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        if (data == null || data.get("userId") == null || data.get("userId").isNull()) {
            return Map.of("error", "userId is required");
        }
        Long userId = data.get("userId").asLong();

        switch (command) {
            case ACCOUNT_GET_IRP_BY_USER: {
                String irp = svc.getIrpAcctNoByUserId(userId);
                Map<String, Object> res = new HashMap<>();
                // ★ IRP 없을 땐 키 자체를 생략 (WAS는 키 없으면 null로 처리)
                if (irp != null && !irp.isBlank()) res.put("irpAcctNo", irp);
                return res;
            }
            case ACCOUNT_GET_DC_LIST_BY_USER: {
                List<String> list = svc.getDcAccountNosByUserId(userId);
                if (list == null) list = Collections.emptyList();  // ★ null 방지
                return Map.of("dcAccounts", list);
            }
            default:
                return Map.of("error", "Unknown ACCOUNT command");
        }
    }
}
