// src/main/java/com/example/memo/purchase/controller/TradeApiController.java
package com.example.memo.purchase.trade_common.rest_controller;

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

    // ===== (기존) 사전 체크/상품/서류/미리보기/적용/변경내역 =====
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
            @RequestParam(name="q", required=false, defaultValue="") String q) {
        JsonNode req = om.createObjectNode().put("category", category).put("q", q);
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
            @RequestParam(name="from", required=false) String from,
            @RequestParam(name="to",   required=false) String to) {
        JsonNode req = om.createObjectNode()
                .put("accountType", accountType)
                .put("acountId", acountId)
                .put("from", from==null? "" : from)
                .put("to",   to==null? "" : to);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PENDING_BUY_HISTORY, req));
        return ResponseEntity.ok(res);
    }

    // ===== (신규) 만기예정 목록 조회 =====
    // UI: “만기예정상품 변경예약” 화면에서 월별/상품별 예금 보유분 보여주기
    @GetMapping("/maturity/list")
    public ResponseEntity<?> maturityList(
            @RequestParam("accountType") String accountType,
            @RequestParam("acountId") String acountId) {
        JsonNode req = om.createObjectNode()
                .put("accountType", accountType)
                .put("acountId", acountId);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.MATURITY_RESERVATION_LIST, req));
        return ResponseEntity.ok(res);
    }

    // ===== (신규) 만기예정 변경 예약(즉시 적용형) =====
    // UI: 특정 예금 보유분 선택 → 전환일/목표배분 입력 → 적용
    @PostMapping("/maturity/apply")
    public ResponseEntity<?> maturityApply(@RequestBody JsonNode body) {
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.MATURITY_RESERVATION_APPLY, body));
        return ResponseEntity.ok(res);
    }
}
