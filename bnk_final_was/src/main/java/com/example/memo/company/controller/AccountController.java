package com.example.memo.company.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/company/account")
@RequiredArgsConstructor
public class AccountController {
    private final TcpClientService tcpService;
    private final ObjectMapper objectMapper;

    @GetMapping("/details/{memberId}")
    public ResponseEntity<?> getAccountDetails(@PathVariable("memberId") Long memberId) {
        ObjectNode data = objectMapper.createObjectNode().put("memberId", memberId);
        TcpMessage msg = new TcpMessage(Command.ACCOUNT_GET_DETAILS, data);
        Object response = tcpService.sendMessage(msg);
        return ResponseEntity.ok(response);
    }
}