// src/main/java/com/example/memo/purchase/change/rest_controller/ChangeSubmitController.java
package com.example.memo.purchase.change.rest_controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.purchase.change.dto.RequestChangeDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/retain-api/change")
@RequiredArgsConstructor
public class ChangeSubmitController {

    private final ObjectMapper mapper;
    private final TcpClientService tcpClientService;

    /**
     * ✅ Step5 최종 제출
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitChange(@RequestBody RequestChangeDto req, HttpSession session) {
        try {
            // 디버깅 로그
            System.out.println("== [/submit] 요청 DTO: " + req);

            JsonNode payload = mapper.valueToTree(req);
            TcpMessage msg = new TcpMessage(Command.CHANGE_UPDATE_LEDGER, payload);
            JsonNode response = tcpClientService.sendMessage(msg);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                java.util.Map.of("ok", false, "error", e.getMessage())
            );
        }
    }
}
