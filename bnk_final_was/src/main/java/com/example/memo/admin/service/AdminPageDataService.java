package com.example.memo.admin.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPageDataService {

    private final TcpClientService tcpService;
    private final ObjectMapper objectMapper;

    public void populateFor(String code, Model model) {
        if (!"total-assets".equals(code)) return;

        JsonNode res = tcpService.sendMessage(new TcpMessage(
                Command.ADMIN_STATS_OVERVIEW,
                objectMapper.createObjectNode()
        ));

        if (res == null || !res.has("result")) {
            populateDefaults(model);
            return;
        }

        JsonNode result = res.get("result");

        // ✅ [수정] "totals" -> "totalAssetsByType" 으로 키 이름 변경
        JsonNode totalsNode = result.path("totalAssetsByType");
        
        // ✅ [수정] DTO Record에 정의된 원래 필드명으로 되돌리기
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("irpTotal",     dec(totalsNode.path("IRP").path("totalAsset")));
        totals.put("irpCash",      dec(totalsNode.path("IRP").path("cashTotal")));
        totals.put("irpPrincipal", dec(totalsNode.path("IRP").path("principalTotal")));
        totals.put("irpFund",      dec(totalsNode.path("IRP").path("fundTotal")));

        totals.put("dcTotal",      dec(totalsNode.path("DC").path("totalAsset")));
        totals.put("dcCash",       dec(totalsNode.path("DC").path("cashTotal")));
        totals.put("dcPrincipal",  dec(totalsNode.path("DC").path("principalTotal")));
        totals.put("dcFund",       dec(totalsNode.path("DC").path("fundTotal")));
        
        model.addAttribute("totals", totals);

        try {
            String allocationJson = objectMapper.writeValueAsString(result.path("allocation"));
            String riskJson = objectMapper.writeValueAsString(result.path("riskSplit"));
            model.addAttribute("allocationJson", allocationJson);
            model.addAttribute("riskJson", riskJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize stats data to JSON", e);
            model.addAttribute("allocationJson", "[]");
            model.addAttribute("riskJson", "[]");
        }
    }

    private void populateDefaults(Model model) {
        Map<String, Object> totals = Map.of(
            "irpTotal", BigDecimal.ZERO, "irpCash", BigDecimal.ZERO,
            "irpPrincipal", BigDecimal.ZERO, "irpFund", BigDecimal.ZERO,
            "dcTotal", BigDecimal.ZERO, "dcCash", BigDecimal.ZERO,
            "dcPrincipal", BigDecimal.ZERO, "dcFund", BigDecimal.ZERO
        );
        model.addAttribute("totals", totals);
        model.addAttribute("allocationJson", "[]");
        model.addAttribute("riskJson", "[]");
    }

    private static BigDecimal dec(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return BigDecimal.ZERO;
        if (n.isNumber()) return n.decimalValue();
        try {
            return new BigDecimal(n.asText("0").replace(",", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}