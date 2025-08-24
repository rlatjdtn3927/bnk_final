package com.example.memo.retention;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
@RequestMapping("/retain-api/account")
@RequiredArgsConstructor
public class RetentionRestController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper mapper;

    /**
     * 계좌 최소 목록
     * 응답: { ok: true, accounts:[{type,id}], default:{type,id} }
     */
    @GetMapping("/list-min")
    public ResponseEntity<?> listMin(HttpSession session) {
        Long userId = (Long) session.getAttribute("user");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "error", "로그인이 필요합니다."));
        }

        ObjectNode payload = mapper.createObjectNode().put("userId", userId);
        TcpMessage message  = new TcpMessage(Command.RETAIN_GET_ACCOUNTS, payload);
        JsonNode apRes      = tcpClientService.sendMessage(message); // AP: RetainHandler -> RetainAccountService

        if (apRes == null || apRes.isNull() || apRes.hasNonNull("error")) {
            String err = (apRes != null && apRes.hasNonNull("error"))
                    ? apRes.get("error").asText() : "AP 무응답";
            return ResponseEntity.ok(Map.of("ok", false, "error", "계좌 목록 조회 실패: " + err));
        }

        JsonNode result   = apRes.get("resultList"); // { accounts, defaults }
        JsonNode accounts = (result != null && result.has("accounts")) ? result.get("accounts") : mapper.createArrayNode();
        JsonNode defaults = (result != null && result.has("defaults")) ? result.get("defaults") : mapper.nullNode();

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "accounts", accounts,
                "default", defaults   // 프론트는 res.default 사용
        ));
    }
}
