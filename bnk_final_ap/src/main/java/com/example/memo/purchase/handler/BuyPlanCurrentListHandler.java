// AP - src/main/java/com/example/memo/purchase/handler/BuyPlanCurrentListHandler.java
package com.example.memo.purchase.handler;

import com.example.memo.purchase.service.BuyPlanQueryService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BuyPlanCurrentListHandler implements TcpMessageHandler {

    private final BuyPlanQueryService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("BUY_PLAN_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        // { "userId": 1001 } 형태 기대
        long userId = data != null && data.has("userId") ? data.get("userId").asLong() : 0L;

        // service는 이미 구현되어 있다고 가정 (List<Map<String, Object>> 반환)
        var items = service.getCurrentPlan(userId);

        // Tcp 서버가 Object를 { data: <Object> }로 감싸주는 구조라면
        // 컨트롤러에서 res.path("data")로 접근 가능.
        Map<String, Object> payload = new HashMap<>();
        payload.put("items", items);
        return payload;
    }
}
