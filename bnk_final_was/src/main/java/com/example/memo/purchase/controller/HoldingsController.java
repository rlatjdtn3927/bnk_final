package com.example.memo.purchase.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.purchase.dto.ApiResponse;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/holdings")
public class HoldingsController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    // 요약
    @GetMapping("/summary")
    public ResponseEntity<?> summary(
            @RequestParam String accountType,
            @RequestParam String acountId) {
        JsonNode req = om.createObjectNode()
                .put("accountType", accountType)
                .put("acountId", acountId);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.SUMMARY_GET, req));
        return ResponseEntity.ok(new ApiResponse<>(res));
    }

    // 거래내역(상품)
    @GetMapping("/transactions")
    public ResponseEntity<?> transactions(
            @RequestParam String accountType,
            @RequestParam String acountId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        JsonNode req = om.createObjectNode()
                .put("accountType", accountType)
                .put("acountId", acountId)
                .put("from", from == null ? "" : from)
                .put("to", to == null ? "" : to);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.TXN_LIST, req));
        return ResponseEntity.ok(new ApiResponse<>(res));
    }

    // 계좌입금내역
    @GetMapping("/deposits")
    public ResponseEntity<?> deposits(
            @RequestParam String accountType,
            @RequestParam String acountId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        JsonNode req = om.createObjectNode()
                .put("accountType", accountType)
                .put("acountId", acountId)
                .put("from", from == null ? "" : from)
                .put("to", to == null ? "" : to);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.DEPOSIT_LIST, req));
        return ResponseEntity.ok(new ApiResponse<>(res));
    }

    // 계좌 입금 (즉시 반영)
    @PostMapping("/deposit")
    public ResponseEntity<?> createDeposit(@RequestBody JsonNode body) {
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.DEPOSIT_CREATE, body));
        return ResponseEntity.ok(new ApiResponse<>(res));
    }
}
