package com.example.memo.retention;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/retention")
@RequiredArgsConstructor
public class RetentionRestController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper om;

    /** 사용자 보유 계좌 목록 */
    @GetMapping("/accounts")
    public JsonNode getAccounts(HttpSession session) {
        Long userId = (Long) session.getAttribute("user");
        if (userId == null) userId = 1L; // 임시

        ObjectNode data = om.createObjectNode().put("userId", userId);
        TcpMessage msg = new TcpMessage(Command.RETAIN_GET_ACCOUNTS, data);
        return tcpClientService.sendMessage(msg); // { ok, accounts:[] }
    }

    /** 계좌 보유현황(현재 시점 스냅샷) */
    @GetMapping("/holdings")
    public JsonNode getHoldings(@RequestParam("type") String accountType,
                                @RequestParam("id") String accountId) {
        ObjectNode data = om.createObjectNode()
                .put("accountType", accountType) // "IRP" | "DC"
                .put("accountId", accountId);

        TcpMessage msg = new TcpMessage(Command.CHANGE_GET_HOLDINGS, data);
        return tcpClientService.sendMessage(msg); // { resultList:{ fundHoldings[], principalLedgers[] } } or { error }
    }
    
    /** 계좌 요약 (메인 페이지용) */
    @GetMapping("/accounts-summary")
    public JsonNode getAccountsSummary(HttpSession session) {
        Long userId = (Long) session.getAttribute("user");
        if (userId == null) userId = 1L;

        ObjectNode data = om.createObjectNode().put("userId", userId);
        TcpMessage msg = new TcpMessage(Command.RETAIN_GET_ACCOUNTS_SUMMARY, data);
        return tcpClientService.sendMessage(msg);
    }
}
