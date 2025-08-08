package com.example.memo.company.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.company.dto.AccountRequestDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/company/dc")
public class AccountRequestController {

    private final TcpClientService tcpService;
    private final ObjectMapper objectMapper;

    public AccountRequestController(TcpClientService tcpService, ObjectMapper objectMapper) {
        this.tcpService = tcpService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/account/request")
    public ResponseEntity<?> requestAccountOpening(@RequestBody AccountRequestDto requestDto) {
        // 1. 실행할 Command 정의
        Command cmd = Command.ACCOUNT_REQUEST_CREATE;

        // 2. DTO를 JsonNode 데이터로 변환
        JsonNode data = objectMapper.convertValue(requestDto, JsonNode.class);

        // 3. TcpMessage 객체 생성
        TcpMessage message = new TcpMessage(cmd, data);

        // 4. TCP 서비스를 통해 AP로 메시지 전송 및 결과 반환
        Object response = tcpService.sendMessage(message); // sendMessage는 TcpMessage를 직렬화하여 전송
        
        return ResponseEntity.ok(response);
    }
}