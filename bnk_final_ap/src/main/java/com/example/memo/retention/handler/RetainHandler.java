package com.example.memo.retention.handler;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.memo.retention.service.AccountSummaryService;
import com.example.memo.retention.service.RetainAccountService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RetainHandler implements TcpMessageHandler {

    @Autowired
    private RetainAccountService retainAccountService;
    
    @Autowired
    private AccountSummaryService accountSummaryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(Command command) {
        // RETAIN_ 로 시작하는 모든 명령 처리 가능
        return command.name().startsWith("RETAIN_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            switch (command) {
                case RETAIN_GET_ACCOUNTS: {
                    Long userId = data.hasNonNull("userId") ? data.get("userId").asLong() : null;
                    if (userId == null) {
                        return Map.of("ok", false, "error", "userId가 없습니다.");
                    }

                    var accounts = retainAccountService.findAccountsOfUser(userId);

                    if (accounts == null) accounts = java.util.Collections.emptyList();

                    return Map.of("ok", true, "accounts", accounts);
                }
                case RETAIN_GET_ACCOUNTS_SUMMARY: {
                    Long userId = data.hasNonNull("userId") ? data.get("userId").asLong() : null;
                    if (userId == null) {
                        return Map.of("ok", false, "error", "userId가 없습니다.");
                    }

                    // AP 비즈니스 로직 호출 → AccountsResponse 생성
                    var summary = accountSummaryService.getAccounts(userId);

                    return Map.of("ok", true, "data", summary);
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