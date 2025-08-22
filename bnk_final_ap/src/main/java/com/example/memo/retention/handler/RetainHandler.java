package com.example.memo.retention.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.memo.retention.dto.ResponseAccountListMinDto;
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
                    // RetainAccountService 는 JsonNode 를 그대로 받아 처리
                    ResponseAccountListMinDto result = retainAccountService.listAccountsMin(data);
                    if (result == null) {
                        return "계좌 목록 조회 실패";
                    }
                    // AP 표준 응답 포맷: { "resultList": ... }
                    return java.util.Map.of("resultList", result);
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