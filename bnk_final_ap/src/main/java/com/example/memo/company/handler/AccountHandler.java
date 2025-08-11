package com.example.memo.company.handler;

import org.springframework.stereotype.Component;

import com.example.memo.company.dto.DcAccountDetailsDto;
import com.example.memo.company.service.AccountService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountHandler implements TcpMessageHandler {
    private final AccountService accountService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Command command) {
        return command == Command.ACCOUNT_GET_DETAILS;
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        Long memberId = data.get("memberId").asLong();
        DcAccountDetailsDto details = accountService.getAccountDetailsByMemberId(memberId);
        return objectMapper.convertValue(details, JsonNode.class);
    }
}