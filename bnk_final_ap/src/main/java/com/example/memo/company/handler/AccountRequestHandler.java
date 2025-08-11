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
        // "ACCOUNT_REQUEST_"로 시작하는 모든 Command를 이 핸들러가 처리
        return command.name().startsWith("ACCOUNT_REQUEST_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case ACCOUNT_REQUEST_CREATE:
                // 1. 기존처럼 memberIds를 추출
                List<String> memberIds = objectMapper.convertValue(data.get("memberIds"), new TypeReference<>() {});
                
                // 2. JsonNode에서 requestedById 추출
                Long requestedById = data.get("requestedById").asLong();
                
                // 3. 서비스 호출 시 두 개의 파라미터 모두 전달
                int successCount = accountRequestService.createAccountRequests(memberIds, requestedById);
                
                return Map.of("message", "Request received.", "successCount", successCount);
            case ACCOUNT_REQUEST_GET_PENDING:
                // 그룹핑된 데이터 가져오기
                Map<String, List<AccountRequestListDto>> groupedRequests = accountRequestService.getPendingRequestsGroupedByCompany();
                // 결과를 JsonNode로 변환하여 반환
                return objectMapper.convertValue(groupedRequests, JsonNode.class);
            case ACCOUNT_REQUEST_APPROVE_BULK: {
                List<Long> requestIds = objectMapper.convertValue(data.get("requestIds"), new TypeReference<>() {});
                Long approverId = data.get("approverId").asLong();
                accountRequestService.approveRequests(requestIds, approverId);
                return createSuccessResponse(requestIds.size() + "건이 성공적으로 승인되었습니다.");
            }
                
            case ACCOUNT_REQUEST_REJECT_BULK: {
                List<Long> requestIds = objectMapper.convertValue(data.get("requestIds"), new TypeReference<>() {});
                Long approverId = data.get("approverId").asLong();
                String reason = data.get("reason").asText();
                accountRequestService.rejectRequests(requestIds, approverId, reason);
                return createSuccessResponse(requestIds.size() + "건이 거절 처리되었습니다.");
            }
            default:
                return Map.of("error", "Unsupported command in AccountRequestHandler: " + command.name());
        }
    }
    
    /**
     * 공통 성공 응답(JsonNode)을 생성하는 헬퍼 메서드
     * @param message 클라이언트에 보낼 메시지
     * @return {"message": "..."} 형태의 JsonNode
     */
    private JsonNode createSuccessResponse(String message) {
        return objectMapper.createObjectNode().put("message", message);
    }
}