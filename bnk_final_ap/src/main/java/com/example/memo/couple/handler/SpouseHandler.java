package com.example.memo.couple.handler;

import org.springframework.stereotype.Component;

import com.example.memo.couple.service.SpouseLinkService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpouseHandler implements TcpMessageHandler {

    private final SpouseLinkService spouseLinkService; // 이미 만든 서비스
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Command command) {
        // SUBSCRIBER_* 스타일과 동일: 접두어 매칭
        return command.name().startsWith("SPOUSE_LINK_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            switch (command) {
                case SPOUSE_LINK_APPLY: {
                    Long applicantUserId = data.path("applicantUserId").asLong();
                    String spouseName    = data.path("spouseName").asText(null);
                    String spouseBirth   = data.path("spouseBirth").asText(null);

                    String filename = null, contentType = null, base64 = null;
                    JsonNode f = data.get("file");
                    if (f != null && !f.isNull()) {
                        filename    = f.path("filename").asText(null);
                        contentType = f.path("contentType").asText(null);
                        base64      = f.path("base64").asText(null);
                    }
                    
                    Long existingLinkId = data.has("existingLinkId") ? data.get("existingLinkId").asLong() : null;

                    // 서비스 호출 → ObjectNode 반환 (그대로 리턴)
                    ObjectNode res = spouseLinkService.apply(
                            applicantUserId, spouseName, spouseBirth,
                            filename, contentType, base64, existingLinkId
                    );
                    return res;
                }
                case SPOUSE_LINK_DECIDE: {
                    Long linkId = data.path("linkId").asLong();
                    Long spouseUserId = data.path("spouseUserId").asLong();
                    String action = data.path("action").asText();

                    ObjectNode res = spouseLinkService.decideBySpouse(linkId, spouseUserId, action);
                    return objectMapper.createObjectNode()
                            .put("success", true)
                            .put("code", "OK")
                            .set("data", res);
                }
                case SPOUSE_LINK_REQUEST_ADMIN_REVIEW: {
                    Long linkId = data.path("linkId").asLong();
                    String spouseName  = data.path("spouseName").asText();
                    String spouseBirth = data.path("spouseBirth").asText();

                    String filename = null, contentType = null, base64 = null;
                    JsonNode f = data.get("file");
                    if (f != null && !f.isNull()) {
                        filename    = f.path("filename").asText(null);
                        contentType = f.path("contentType").asText(null);
                        base64      = f.path("base64").asText(null);
                    }

                    spouseLinkService.requestAdminReview(linkId, spouseName, spouseBirth, filename, contentType, base64);

                    return objectMapper.createObjectNode()
                            .put("success", true)
                            .put("code", "OK")
                            .put("message", "관리자 검토를 요청했습니다.");
                }
                case SPOUSE_LINK_STATUS: {
                    Long userId = data.path("userId").asLong();
                    ObjectNode d = spouseLinkService.getStatus(userId);
                    return objectMapper.createObjectNode()
                            .put("success", true)
                            .put("code", "OK")
                            .set("data", d);
                }
                case SPOUSE_LINK_GET_DETAILS: {
                    Long linkId = data.path("linkId").asLong();
                    ObjectNode details = spouseLinkService.getRequestDetails(linkId);
                    return details;
                }
                default:
                    return "알 수 없는 명령: " + command.name();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "처리 중 오류 발생: " + e.getMessage();
        }
    }
}