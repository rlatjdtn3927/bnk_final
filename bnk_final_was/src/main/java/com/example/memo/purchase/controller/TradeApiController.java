// src/main/java/com/example/memo/purchase/web/TradeApiController.java
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
@RequestMapping("/purchase/api/trade")
@RequiredArgsConstructor
public class TradeApiController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    @GetMapping("/check")
    public ResponseEntity<?> prereq(
            @RequestParam("userId") String userId,
            @RequestParam("accountType") String accountType,
            @RequestParam("acountId") String acountId) {
        JsonNode req = om.createObjectNode()
                .put("userId", userId)
                .put("accountType", accountType)
                .put("acountId", acountId);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.TRADE_PREREQ_CHECK, req));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/products")
    public ResponseEntity<?> products(
            @RequestParam("category") String category,
            @RequestParam(name= "q", required = false, defaultValue = "") String q) {
    	//q는 상품검색어-> 프론트에서 입력하는 “상품명/코드” 텍스트
        JsonNode req = om.createObjectNode()
                .put("category", category)
                .put("q", q);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PRODUCT_SEARCH, req));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/documents")
    public ResponseEntity<?> documents(@RequestBody JsonNode body) {
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.DOCUMENT_LIST, body));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody JsonNode body) {
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.ALLOCATION_PREVIEW, body));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody JsonNode body) {
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.ALLOCATION_APPLY, body));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/pending/history")
    public ResponseEntity<?> pendingHistory(
    		@RequestParam("accountType") String accountType,
            @RequestParam("acountId") String acountId,
            @RequestParam(name = "from",required = false) String from,
            @RequestParam(name= "to",required = false) String to) {
        JsonNode req = om.createObjectNode()
                .put("accountType", accountType)
                .put("acountId", acountId)
                .put("from", from == null ? "" : from)
                .put("to", to == null ? "" : to);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PENDING_BUY_HISTORY, req));
        return ResponseEntity.ok(res);
    }
}
