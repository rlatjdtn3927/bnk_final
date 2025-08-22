package com.example.memo.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportController {
	
	private final TcpClientService tcpClientService;
    private final ObjectMapper objectMapper;
    
    /**
     * 월별 가입자 수 조회 API
     * 예: GET /admin/dashboard/monthly?fromYm=2025-01&toYm=2025-08
     */
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlySubscribers(
            @RequestParam("fromYm") String fromYm,
            @RequestParam("toYm") String toYm) {

        ObjectNode req = objectMapper.createObjectNode()
                .put("fromYm", fromYm)
                .put("toYm", toYm);

        Object res = tcpClientService.sendMessage(
                new TcpMessage(Command.REPORT_MONTHLY_SUBSCRIBERS, req)
        );
        return ResponseEntity.ok(res);
    }
}
