package com.example.memo.admin.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.admin.dto.BankEmployeeLoginDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/admin")
public class BankEmployeeController {

    @Autowired
    private TcpClientService tcpService;
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/login")
    public ResponseEntity<?> login(BankEmployeeLoginDto loginDto, HttpServletRequest request) {
        try {
            JsonNode data = objectMapper.convertValue(loginDto, JsonNode.class);
            TcpMessage msg = new TcpMessage(Command.BANK_EMPLOYEE_LOGIN, data);
            JsonNode responseNode = (JsonNode) tcpService.sendMessage(msg);

            if (responseNode != null && !responseNode.isNull()) {
                // 1. AP 서버의 응답(JsonNode)을 DTO 객체로 변환
                BankEmployeeLoginDto loginEmployee = objectMapper.treeToValue(responseNode, BankEmployeeLoginDto.class);

                // 2. 세션을 가져와 DTO 객체를 저장
                HttpSession session = request.getSession(true);
                session.setAttribute("loginEmployee", loginEmployee);
                session.setMaxInactiveInterval(1800); // 30분

                return ResponseEntity.ok().body(Map.of("redirectUrl", "/admin"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                     .body(Map.of("message", "아이디 또는 비밀번호가 일치하지 않습니다."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "로그인 처리 중 오류가 발생했습니다."));
        }
    }
}