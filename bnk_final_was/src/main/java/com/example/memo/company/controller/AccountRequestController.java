package com.example.memo.company.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.company.dto.AccountRequestDto;
import com.example.memo.company.dto.CompanyLoginResponseDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;

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
    public ResponseEntity<?> requestAccountOpening(@RequestBody AccountRequestDto requestDto, HttpSession session) {
        
        // 1. 세션에서 로그인 정보를 가져와 요청자 ID 확인
        CompanyLoginResponseDto loginManager = (CompanyLoginResponseDto) session.getAttribute("loginManager");
        if (loginManager == null) {
            return ResponseEntity.status(401).body("세션이 만료되었습니다. 다시 로그인해주세요.");
        }
        Long requestedById = loginManager.getManagerId(); // 세션에서 기업 담당자의 ID 추출

        // 2. AP에 보낼 데이터에 요청자 ID를 추가
        ObjectNode data = objectMapper.convertValue(requestDto, ObjectNode.class);
        data.put("requestedById", requestedById);

        // 3. TCP 메시지를 생성하고 전송
        Command cmd = Command.ACCOUNT_REQUEST_CREATE;
        TcpMessage message = new TcpMessage(cmd, data);
        Object response = tcpService.sendMessage(message);
        
        return ResponseEntity.ok(response);
    }
}