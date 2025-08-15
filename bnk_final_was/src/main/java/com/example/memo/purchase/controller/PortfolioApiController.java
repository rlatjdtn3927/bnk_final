// src/main/java/com/example/memo/purchase/web/PortfolioApiController.java
package com.example.memo.purchase.controller;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase/api/portfolio")
@RequiredArgsConstructor
public class PortfolioApiController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    @GetMapping
    public ResponseEntity<?> list(
    		@RequestParam("accountType") String accountType,
            @RequestParam("acountId") String acountId) {
        JsonNode req = om.createObjectNode()
                .put("accountType", accountType)
                .put("acountId", acountId);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PORTFOLIO_LIST, req));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/change/preview")
    public ResponseEntity<?> changePreview(@RequestBody JsonNode body) {
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PORTFOLIO_CHANGE_PREVIEW, body));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/change/apply")
    public ResponseEntity<?> changeApply(@RequestBody JsonNode body) {
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PORTFOLIO_CHANGE_APPLY, body));
        return ResponseEntity.ok(res);
    }
}
