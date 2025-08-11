package com.example.memo.branch.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.memo.branch.dto.BranchNearbyRequest;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BranchController {

    private final TcpClientService tcpService;
    private final ObjectMapper objectMapper;

    // 프런트가 호출하는 엔드포인트 (POST /branches/nearby)
    @PostMapping("/branches/nearby")
    @ResponseBody
    public ResponseEntity<?> nearby(@RequestBody BranchNearbyRequest req) {
        JsonNode data = objectMapper.convertValue(req, JsonNode.class);
        TcpMessage msg = new TcpMessage(Command.BRANCH_NEARBY, data);
        JsonNode resp = tcpService.sendMessage(msg);
        return ResponseEntity.status(HttpStatus.OK).body(tcpService.sendMessage(msg));
    }

    // 페이지 라우팅(이미 있으면 생략)
    @GetMapping("/branches/page")
    public String page() { return "branches/page"; }
}
