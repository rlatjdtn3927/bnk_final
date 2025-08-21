// src/main/java/com/example/memo/purchase/controller/management/rest_controller/Step1AccountAPIController.java
package com.example.memo.purchase.trade_common.rest_controller;

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

import java.util.*;

@RestController
@RequestMapping("/purchase/api/accounts")
@RequiredArgsConstructor
public class Step1AccountAPIController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    @GetMapping("/step1")
    public ResponseEntity<Map<String,Object>> getAccountsForStep1(
            @RequestParam(name = "userId", required = false) Long userIdParam,
            @RequestParam(name = "dcMemberId", required = false) Long dcMemberIdParam,
            HttpSession session) {

        try {
            Long userId = (userIdParam != null) ? userIdParam : (Long) session.getAttribute("userId");
            Long dcMemberId = (dcMemberIdParam != null) ? dcMemberIdParam : (Long) session.getAttribute("dcMemberId");
            if (userId == null) userId = 1001L; // fallback

            ObjectNode req = om.createObjectNode().put("userId", userId);
            if (dcMemberId != null) req.put("dcMemberId", dcMemberId);

            JsonNode res = tcp.sendMessage(new TcpMessage(Command.ACCOUNT_OVERVIEW_GET, req));
            JsonNode data = res.path("data");
            if (data.has("data")) data = data.get("data");

            // IRP
            boolean hasIrp = data.path("irp").path("exists").asBoolean(false);
            Map<String,Object> irpItem = null;
            if (hasIrp) {
                JsonNode irp = data.path("irp").path("account");
                String irpNo = irp.path("irpAcctNo").asText(null);
                irpItem = new LinkedHashMap<>();
                irpItem.put("accountId",   irpNo);
                irpItem.put("accountType", "IRP");
                irpItem.put("displayName", mask(irpNo));
                irpItem.put("status",      irp.path("status").asText("NORMAL"));
            }

            // DC
            boolean hasDc = data.path("dc").path("exists").asBoolean(false);
            List<Map<String,Object>> dcList = new ArrayList<>();
            if (hasDc) {
                for (JsonNode dc : data.path("dc").path("accounts")) {
                    String no = dc.path("accountNo").asText(null);
                    Map<String,Object> item = new LinkedHashMap<>();
                    item.put("accountId",   no);
                    item.put("accountType", "DC");
                    item.put("displayName", mask(no));
                    item.put("status",      dc.path("status").asText("NORMAL"));
                    dcList.add(item);
                }
            }

            String message = data.path("message").asText(null);

            Map<String,Object> body = new LinkedHashMap<>();
            body.put("hasIrp", hasIrp);
            body.put("irp",    irpItem);
            body.put("hasDc",  hasDc);
            body.put("dcList", dcList);
            body.put("message", message);

            // (선택) 현재 플로우 배지 표시용: 세션의 PassValueDto가 있으면 flow도 내려줄 수 있음
            Object pass = session.getAttribute("PassValueDto");
            if (pass != null) {
                // 프론트에서 쓸 수도 있으니 flow만 보너스로 실어줌(없어도 무방)
                try {
                    String flow = (String) pass.getClass().getMethod("getFlow").invoke(pass);
                    body.put("flow", flow);
                } catch (Exception ignore) {}
            }

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            Map<String,Object> err = new LinkedHashMap<>();
            err.put("hasIrp", false);
            err.put("hasDc",  false);
            err.put("message","계좌 조회 오류: " + e.getMessage());
            return ResponseEntity.ok(err);
        }
    }

    private String mask(String acctNo) {
        if (acctNo == null || acctNo.length() < 4) return String.valueOf(acctNo);
        return "****-" + acctNo.substring(acctNo.length() - 4);
    }
}
