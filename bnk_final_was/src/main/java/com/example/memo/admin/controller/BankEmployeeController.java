package com.example.memo.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.admin.dto.LoginRequestDto;
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
    public ResponseEntity<?> login(LoginRequestDto loginDto, HttpServletRequest request) {
        
        JsonNode data = objectMapper.convertValue(loginDto, JsonNode.class);
        TcpMessage msg = new TcpMessage(Command.BANK_EMPLOYEE_LOGIN, data);
        JsonNode responseNode = (JsonNode) tcpService.sendMessage(msg);

        if (responseNode != null && !responseNode.isNull()) {
            HttpSession session = request.getSession(true);
            session.setAttribute("loggedInEmployee", responseNode);
            session.setMaxInactiveInterval(1800);
            
            // 성공 시 메인 페이지로 리디렉션하라는 의미로 URL을 보낼 수도 있습니다.
            return ResponseEntity.ok().body("{\"redirectUrl\":\"/admin\"}");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body("아이디 또는 비밀번호가 일치하지 않습니다.");
        }
    }
}