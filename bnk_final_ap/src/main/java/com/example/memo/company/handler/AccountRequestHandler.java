package com.example.memo.company.handler;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.company.service.AccountRequestService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component // Spring이 Bean으로 등록하도록 설정
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
                // 1. 기존처럼 memberIds를 추출합니다.
                List<String> memberIds = objectMapper.convertValue(data.get("memberIds"), new TypeReference<>() {});
                
                // 2. (추가된 부분) JsonNode에서 requestedById도 추출합니다.
                Long requestedById = data.get("requestedById").asLong();
                
                // 3. (수정된 부분) 서비스 호출 시 두 개의 파라미터를 모두 전달합니다.
                int successCount = accountRequestService.createAccountRequests(memberIds, requestedById);
                
                return Map.of("message", "Request received.", "successCount", successCount);
                
            default:
                return Map.of("error", "Unsupported command in AccountRequestHandler: " + command.name());
        }
    }
}