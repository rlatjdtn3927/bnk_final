package com.example.memo.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/api/stats")
@RequiredArgsConstructor
public class AdminStatsRestController {

    private final TcpClientService tcpService;
    private final ObjectMapper objectMapper;

    /**
     * 통합 통계 (총자산 + 상품별 운용비중 + 위험/안전 비중)
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getOverview() {
        Command cmd = Command.ADMIN_STATS_OVERVIEW;
        JsonNode data = objectMapper.createObjectNode(); // 요청 바디는 비어 있음
        TcpMessage msg = new TcpMessage(cmd, data);

        JsonNode response = tcpService.sendMessage(msg);
        return ResponseEntity.ok(response);
    }

    /**
     * 총자산만 조회
     */
    @GetMapping("/total")
    public ResponseEntity<?> getTotal() {
        Command cmd = Command.ADMIN_STATS_TOTAL;
        JsonNode data = objectMapper.createObjectNode();
        TcpMessage msg = new TcpMessage(cmd, data);

        JsonNode response = tcpService.sendMessage(msg);
        return ResponseEntity.ok(response);
    }

    /**
     * 상품별 운용비중만 조회
     */
    @GetMapping("/allocation")
    public ResponseEntity<?> getAllocation() {
        Command cmd = Command.ADMIN_STATS_ALLOCATION;
        JsonNode data = objectMapper.createObjectNode();
        TcpMessage msg = new TcpMessage(cmd, data);

        JsonNode response = tcpService.sendMessage(msg);
        return ResponseEntity.ok(response);
    }

    /**
     * 위험/안전 비중만 조회
     */
    @GetMapping("/risk")
    public ResponseEntity<?> getRisk() {
        Command cmd = Command.ADMIN_STATS_RISK;
        JsonNode data = objectMapper.createObjectNode();
        TcpMessage msg = new TcpMessage(cmd, data);

        JsonNode response = tcpService.sendMessage(msg);
        return ResponseEntity.ok(response);
    }
}