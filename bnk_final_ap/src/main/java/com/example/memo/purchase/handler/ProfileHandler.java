package com.example.memo.purchase.handler;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.example.memo.purchase.service.ProfileQueryService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProfileHandler implements TcpMessageHandler {

    private final ProfileQueryService svc;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("PROFILE_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        // 🔒 방어: data 또는 userId 누락 시
        if (data == null || data.get("userId") == null || data.get("userId").isNull() || !data.get("userId").canConvertToLong()) {
            return Map.of("error", "userId is required");
        }

        long userId = data.get("userId").asLong();

        return switch (command) {
            case PROFILE_GET_LATEST_TYPE_BY_USER -> {
                var r = svc.getLatestTypeByUserId(userId);
                Map<String, Object> res = new HashMap<>();
                res.put("typeName", r != null ? r.typeName() : null);
                res.put("typeNo",   r != null ? r.typeNo()   : null); // 현재는 typeId를 등급번호 대용
                yield res;
            }
            default -> Map.of("error", "Unknown PROFILE command: " + command);
        };
    }
}
