// src/main/java/com/example/memo/purchase/controller/management/rest_controller/Step1AccountAPIController.java
package com.example.memo.purchase.controller.management.rest_controller;

import com.example.memo.purchase.dto.Step1AccountsRes;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/purchase/api/accounts")
@RequiredArgsConstructor
public class Step1AccountAPIController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    @GetMapping("/step1")
    public ResponseEntity<Step1AccountsRes> getAccountsForStep1(
            @RequestParam(name = "userId", required = false) Long userIdParam,
            @RequestParam(name = "dcMemberId", required = false) Long dcMemberIdParam,
            HttpSession session) {

        try {
            Long userId = (userIdParam != null) ? userIdParam : (Long) session.getAttribute("userId");
            Long dcMemberId = (dcMemberIdParam != null) ? dcMemberIdParam : (Long) session.getAttribute("dcMemberId");

            if (userId == null) userId = 1001L; // fallback

            ObjectNode req = om.createObjectNode().put("userId", userId);
            if (dcMemberId != null) req.put("dcMemberId", dcMemberId);

            TcpMessage msg = new TcpMessage(Command.ACCOUNT_OVERVIEW_GET, req);
            JsonNode res = tcp.sendMessage(msg);

            JsonNode data = res.path("data");
            if (data.has("data")) data = data.get("data");

            // IRP
            boolean hasIrp = data.path("irp").path("exists").asBoolean(false);
            Step1AccountsRes.AccountItem irpItem = null;
            if (hasIrp) {
                JsonNode irp = data.path("irp").path("account");
                String irpNo = irp.path("irpAcctNo").asText(null);
                irpItem = Step1AccountsRes.AccountItem.builder()
                        .accountId(irpNo)
                        .accountType("IRP")
                        .displayName(mask(irpNo))
                        .status(irp.path("status").asText("NORMAL"))
                        .build();
            }

            // DC
            boolean hasDc = data.path("dc").path("exists").asBoolean(false);
            List<Step1AccountsRes.AccountItem> dcList = new ArrayList<>();
            if (hasDc) {
                for (JsonNode dc : data.path("dc").path("accounts")) {
                    String no = dc.path("accountNo").asText();
                    dcList.add(Step1AccountsRes.AccountItem.builder()
                            .accountId(no)
                            .accountType("DC")
                            .displayName(mask(no))
                            .status(dc.path("status").asText("NORMAL"))
                            .build());
                }
            }

            String message = data.path("message").asText(null);
            Step1AccountsRes body = Step1AccountsRes.builder()
                    .hasIrp(hasIrp)
                    .irp(irpItem)
                    .hasDc(hasDc)
                    .dcList(dcList)
                    .message(message)
                    .build();

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            Step1AccountsRes err = Step1AccountsRes.builder()
                    .hasIrp(false).hasDc(false)
                    .message("계좌 조회 오류: " + e.getMessage())
                    .build();
            return ResponseEntity.ok(err);
        }
    }

    private String mask(String acctNo) {
        if (acctNo == null || acctNo.length() < 4) return acctNo;
        return "****-" + acctNo.substring(acctNo.length() - 4);
    }
}
