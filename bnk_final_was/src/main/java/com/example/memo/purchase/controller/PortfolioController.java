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
@RequestMapping("/portfolio")
public class PortfolioController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    // 보유상품 목록
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam String accountType,
            @RequestParam String acountId) {
        JsonNode req = om.createObjectNode()
                .put("accountType", accountType)
                .put("acountId", acountId);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PORTFOLIO_LIST, req));
        return ResponseEntity.ok(new ApiResponse<>(res));
    }

    // 변경 미리보기
    @PostMapping("/change/preview")
    public ResponseEntity<?> changePreview(@RequestBody JsonNode body) {
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PORTFOLIO_CHANGE_PREVIEW, body));
        return ResponseEntity.ok(new ApiResponse<>(res));
    }

    // 변경 적용 (즉시 반영)
    @PostMapping("/change/apply")
    public ResponseEntity<?> changeApply(@RequestBody JsonNode body) {
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PORTFOLIO_CHANGE_APPLY, body));
        return ResponseEntity.ok(new ApiResponse<>(res));
    }
}
