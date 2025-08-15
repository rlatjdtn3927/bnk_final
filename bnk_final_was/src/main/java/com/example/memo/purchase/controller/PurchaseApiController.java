// src/main/java/com/example/memo/purchase/controller/PurchaseApiController.java
package com.example.memo.purchase.controller;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 화면(JS) → WAS → AP(TCP)의 중계 레이어
 * - 프런트는 /purchase/* 로 호출
 * - 여기서 Command로 변환해 AP에 TCP 전송
 */
@RestController
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseApiController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    // 0) 사전 체크: 로그인/계좌 존재/잔액
    @GetMapping("/check")
    public ResponseEntity<?> prereq(
            @RequestParam String userId,
            @RequestParam String accountType, // "IRP" | "DC"
            @RequestParam String acountId     // IRP: 계좌번호(String), DC: dc_account.id(Long) 문자열로 전달
    ) {
        JsonNode req = om.createObjectNode()
                .put("userId", userId)
                .put("accountType", accountType)
                .put("acountId", acountId);
        return ok(Command.TRADE_PREREQ_CHECK, req);
    }

    // 1) 상품 검색
    @GetMapping("/products")
    public ResponseEntity<?> products(
            @RequestParam String category, // FUND|ETF|TDF|PRINCIPAL|CASH
            @RequestParam(required = false, defaultValue = "") String q
    ) {
        JsonNode req = om.createObjectNode()
                .put("category", category)
                .put("q", q);
        return ok(Command.PRODUCT_SEARCH, req);
    }

    // 2) 서류 목록
    @PostMapping("/documents")
    public ResponseEntity<?> documents(@RequestBody JsonNode body) {
        return ok(Command.DOCUMENT_LIST, body);
    }

    // 3) 미리보기(합계100 검증/금액·수량 산출)
    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody JsonNode body) {
        return ok(Command.ALLOCATION_PREVIEW, body);
    }

    // 4) 적용(비밀번호 확인 → 거래/보유/잔액 즉시 반영)
    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody JsonNode body) {
        return ok(Command.ALLOCATION_APPLY, body);
    }

    // 5) 변경내역(날짜별)
    @GetMapping("/pending/history")
    public ResponseEntity<?> pendingHistory(
            @RequestParam String accountType,
            @RequestParam String acountId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        JsonNode req = om.createObjectNode()
                .put("accountType", accountType)
                .put("acountId", acountId)
                .put("from", from == null ? "" : from)
                .put("to", to == null ? "" : to);
        return ok(Command.PENDING_BUY_HISTORY, req);
    }

    // 6) 보유상품 목록 (위저드/보유변경 진입시)
    @GetMapping("/portfolio")
    public ResponseEntity<?> portfolio(
            @RequestParam String accountType,
            @RequestParam String acountId
    ){
        JsonNode req = om.createObjectNode()
                .put("accountType", accountType)
                .put("acountId", acountId);
        return ok(Command.PORTFOLIO_LIST, req);
    }

    // 7) 거래내역/입금 조회 + 입금 등록 (보유현황 탭)
    @GetMapping("/txns")
    public ResponseEntity<?> txns(@RequestParam String accountType, @RequestParam String acountId){
        JsonNode req = om.createObjectNode().put("accountType", accountType).put("acountId", acountId);
        return ok(Command.TXN_LIST, req);
    }

    @GetMapping("/deposits")
    public ResponseEntity<?> deposits(@RequestParam String accountType, @RequestParam String acountId){
        JsonNode req = om.createObjectNode().put("accountType", accountType).put("acountId", acountId);
        return ok(Command.DEPOSIT_LIST, req);
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody JsonNode body){
        return ok(Command.DEPOSIT_CREATE, body);
    }

    // 공통 중계
    private ResponseEntity<?> ok(Command cmd, JsonNode req){
        JsonNode res = tcp.sendMessage(new TcpMessage(cmd, req));
        return ResponseEntity.ok(res);
    }
}
