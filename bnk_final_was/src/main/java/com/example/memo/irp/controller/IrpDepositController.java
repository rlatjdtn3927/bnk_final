// src/main/java/com/example/memo/irp/rest_controller/IrpDepositController.java
package com.example.memo.irp.controller;

import com.example.memo.irp.dto.IrpDepositRequest;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/retain-api/irp")
@RequiredArgsConstructor
public class IrpDepositController {

    private final ObjectMapper mapper;
    private final TcpClientService tcpClientService;

    /**
     * ✅ IRP 계좌 입금 등록
     */
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody IrpDepositRequest req) {
        try {
            System.out.println("== [/irp/deposit] 요청: " + req);

            // DTO → JsonNode 변환
            JsonNode payload = mapper.valueToTree(req);

            // TCP 전송
            TcpMessage msg = new TcpMessage(Command.IRP_ACCOUNT_DEPOSIT, payload);
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
