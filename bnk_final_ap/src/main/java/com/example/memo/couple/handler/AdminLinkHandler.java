package com.example.memo.couple.handler;

import org.springframework.stereotype.Component;

import com.example.memo.couple.service.SpouseLinkService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminLinkHandler implements TcpMessageHandler {

    private final SpouseLinkService spouseLinkService;
    private final ObjectMapper om;

    @Override
    public boolean supports(Command c) {
        // 'SPOUSE_LINK_ADMIN_' 으로 시작하는 명령만 처리하도록 수정
        return c.name().startsWith("ADMIN_SPOUSE_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            switch (command) {
                /*
            case ADMIN_SPOUSE_LINK_LIST: {
                    ArrayNode list = spouseLinkService.adminListPending(10); // presign 10분
                    ObjectNode res = om.createObjectNode();
                    res.put("success", true).put("code", "OK").set("data", list);
                    return res;
                }
                */
            /*
                case ADMIN_SPOUSE_LINK_DECIDE: {
                    long linkId = data.path("linkId").asLong();
                    String action = data.path("action").asText(null);
                    String reason = data.path("reason").asText(null);

                    // 서비스 호출 시, spouseUserId를 파라미터로 추가
                    ObjectNode d = spouseLinkService.adminDecide(linkId, action, reason);
                    
                    ObjectNode res = om.createObjectNode();
                    res.put("success", true).put("code", "OK").set("data", d);
                    return res;
                }
                */
                default: {
                    return "알 수 없는 명령: " + command.name();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ObjectNode err = om.createObjectNode();
            err.put("success", false).put("code", "ERROR").put("message", e.getMessage());
            return err;
        }
    }
}