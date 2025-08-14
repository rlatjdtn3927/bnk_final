package com.example.memo.company.handler;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.company.dto.AccountRequestListDto;
import com.example.memo.company.service.AccountRequestService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AccountRequestHandler implements TcpMessageHandler {

    private final AccountRequestService accountRequestService;
    private final ObjectMapper objectMapper;

    public AccountRequestHandler(AccountRequestService accountRequestService, ObjectMapper objectMapper) {
        this.accountRequestService = accountRequestService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(Command command) {
        // "ACCOUNT_REQUEST_"로 시작하는 모든 Command 처리
        return command.name().startsWith("ACCOUNT_REQUEST_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            switch (command) {
                case ACCOUNT_REQUEST_CREATE: {
                    // memberIds, requestedById 추출
                    List<String> memberIds = objectMapper.convertValue(
                            data.get("memberIds"), new TypeReference<List<String>>() {});
                    Long requestedById = data.hasNonNull("requestedById") ? data.get("requestedById").asLong() : null;

                    int successCount = accountRequestService.createAccountRequests(memberIds, requestedById);
                    return createSuccessResponse("Request received.", "successCount", successCount);
                }

                case ACCOUNT_REQUEST_GET_PENDING: {
                    return toJson(groupByStatus("PENDING"));
                }

                // 승인 목록
                case ACCOUNT_REQUEST_GET_APPROVED: {
                    return toJson(groupByStatus("APPROVED"));
                }

                // 거절 목록
                case ACCOUNT_REQUEST_GET_REJECTED: {
                    return toJson(groupByStatus("REJECTED"));
                }

                case ACCOUNT_REQUEST_APPROVE_BULK: {
                    List<Long> requestIds = objectMapper.convertValue(
                            data.get("requestIds"), new TypeReference<List<Long>>() {});
                    Long approverId = data.hasNonNull("approverId") ? data.get("approverId").asLong() : null;

                    accountRequestService.approveRequests(requestIds, approverId);
                    return createSuccessResponse(requestIds.size() + "건이 성공적으로 승인되었습니다.");
                }

                case ACCOUNT_REQUEST_REJECT_BULK: {
                    List<Long> requestIds = objectMapper.convertValue(
                            data.get("requestIds"), new TypeReference<List<Long>>() {});
                    Long approverId = data.hasNonNull("approverId") ? data.get("approverId").asLong() : null;
                    String reason = data.hasNonNull("reason") ? data.get("reason").asText() : "";

                    accountRequestService.rejectRequests(requestIds, approverId, reason);
                    return createSuccessResponse(requestIds.size() + "건이 거절 처리되었습니다.");
                }

                default:
                    return Map.of("error", "Unsupported command in AccountRequestHandler: " + command.name());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("message", "AP 처리 중 오류가 발생했습니다.", "command", command.name());
        }
    }

    /* ------------------------ 헬퍼 ------------------------ */

    // 상태별 그룹 결과(Map<String companyName, List<Dto>>) 가져오기
    private Map<String, List<AccountRequestListDto>> groupByStatus(String status) {
        // 서비스에 공통 메서드가 없다면:
        //  - getPendingRequestsGroupedByCompany()가 이미 있다면
        //    → status별로 분기하는 공용 메서드(getRequestsGroupedByCompany(String status)) 하나 추가 권장.
        return accountRequestService.getRequestsGroupedByCompany(status);
    }

    private JsonNode toJson(Object value) {
        return objectMapper.valueToTree(value);
    }

    private JsonNode createSuccessResponse(String message) {
        return objectMapper.createObjectNode().put("message", message);
    }

    private JsonNode createSuccessResponse(String message, String key, Object val) {
        return objectMapper.createObjectNode().put("message", message).putPOJO(key, val);
    }
}