// WAS - src/main/java/com/example/memo/purchase/controller/management/rest_controller/Step5CompareProductAPIController.java
package com.example.memo.purchase.controller.management.rest_controller;

import com.example.memo.purchase.dto.trade.FileUrlDto;
import com.example.memo.purchase.dto.trade.PassValueDto;
import com.example.memo.purchase.dto.trade.SourceProductDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/purchase/api/compare")
@RequiredArgsConstructor
public class Step5CompareProductAPIController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    @GetMapping("/step5")
    public ResponseEntity<Map<String,Object>> compareStep5(HttpSession session) {
        PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
        if (dto == null) {
            return ok(Map.of(
                    "flow", null, "before", List.of(), "after", List.of(),
                    "riskGrade", null, "riskGradeNum", null, "message", "세션에 PassValueDto가 없습니다."
            ));
        }

        final String flow = (dto.getFlow() == null) ? "" : dto.getFlow().toUpperCase(Locale.ROOT);
        try {
            // BEFORE
            List<Map<String,Object>> before = new ArrayList<>();
            if ("RESERVE".equals(flow)) {
                Long userId = (Long) session.getAttribute("userId");
                if (userId == null) userId = 1001L;

                JsonNode res = tcp.sendMessage(new TcpMessage(
                        Command.BUY_PLAN_CURRENT_LIST_GET,
                        om.createObjectNode().put("userId", userId)
                ));
                JsonNode data = res.path("data");
                if (data.has("data")) data = data.get("data");

                for (JsonNode it : data.path("items")) {
                    before.add(item(
                            it.path("productId").asText(),
                            it.path("productName").asText(""),
                            it.path("type").asText("FUND"),
                            it.path("percent").isNumber()? it.path("percent").asInt(): null
                    ));
                }
            } else {
                String targetId = dto.getTargetProdId();
                if (targetId != null) before.add(resolveFromSession(targetId, dto.getFileUrlList()));
            }

            // AFTER
            List<Map<String,Object>> after = new ArrayList<>();
            if ("RESERVE".equals(flow)) {
                // 시작: 기존 before를 맵으로
                Map<String, Map<String,Object>> map = new LinkedHashMap<>();
                for (Map<String,Object> b : before) map.put((String)b.get("productId"), new LinkedHashMap<>(b));

                // 제거: targetProdIdList
                if (dto.getTargetProdIdList() != null) {
                    for (String tid : dto.getTargetProdIdList()) {
                        if (tid != null) map.remove(tid);
                    }
                }

                // 추가/교체: sourceProdIdList (상품+비율)
                if (dto.getSourceProdIdList() != null) {
                    for (Map<SourceProductDto,Integer> m : dto.getSourceProdIdList()) {
                        if (m == null) continue;
                        for (Map.Entry<SourceProductDto,Integer> e : m.entrySet()) {
                            SourceProductDto sp = e.getKey();
                            if (sp == null || sp.getProductId() == null) continue;
                            int pct = e.getValue() == null ? 0 : e.getValue();
                            map.put(sp.getProductId(), item(
                                    sp.getProductId(),
                                    (sp.getProductName() != null ? sp.getProductName() : guessName(sp.getProductId(), dto.getFileUrlList())),
                                    typeOf(sp.getProductId()),
                                    pct
                            ));
                        }
                    }
                }
                after.addAll(map.values());
            } else {
                String sid = dto.getSourceProdId();
                if (sid != null) after.add(resolveFromSession(sid, dto.getFileUrlList()));
            }

            return ok(Map.of(
                    "flow", flow,
                    "before", before,
                    "after", after,
                    "riskGrade", dto.getRiskGrade(),
                    "riskGradeNum", dto.getRiskGradeNum(),
                    "message", null
            ));
        } catch (Exception e) {
            return ok(Map.of(
                    "flow", flow, "before", List.of(), "after", List.of(),
                    "riskGrade", dto.getRiskGrade(), "riskGradeNum", dto.getRiskGradeNum(),
                    "message", "비교 데이터 생성 실패: " + e.getMessage()
            ));
        }
    }

    private Map<String,Object> item(String id, String name, String type, Integer pct){
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("productId", id);
        m.put("productName", name);
        m.put("type", type);
        m.put("percent", pct);
        return m;
    }

    private String typeOf(String productId){
        return (productId != null && productId.startsWith("PG")) ? "PG" : "FUND";
    }

    private String guessName(String productId, List<FileUrlDto> files){
        if (files != null) {
            for (FileUrlDto f : files) {
                if (productId.equals(f.getProdId()) && f.getProdName() != null) return f.getProdName();
            }
        }
        return productId;
    }

    private Map<String,Object> resolveFromSession(String productId, List<FileUrlDto> files){
        return item(productId, guessName(productId, files), typeOf(productId), null);
    }

    private ResponseEntity<Map<String,Object>> ok(Map<String,Object> body){
        return ResponseEntity.ok(body);
    }
}
