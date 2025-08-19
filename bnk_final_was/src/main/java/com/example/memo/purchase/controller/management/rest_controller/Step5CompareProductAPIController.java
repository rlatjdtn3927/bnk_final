// WAS - src/main/java/com/example/memo/purchase/controller/management/rest_controller/Step5CompareProductAPIController.java
package com.example.memo.purchase.controller.management.rest_controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.purchase.dto.trade.CompareItemDto;
import com.example.memo.purchase.dto.trade.FileUrlDto;
import com.example.memo.purchase.dto.trade.PassValueDto;
import com.example.memo.purchase.dto.trade.SourceProductDto;
import com.example.memo.purchase.dto.trade.Step5CompareResDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/purchase/api/compare")
@RequiredArgsConstructor
public class Step5CompareProductAPIController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    @GetMapping("/step5")
    public ResponseEntity<Step5CompareResDto> compareStep5(HttpSession session) {
        PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
        if (dto == null) {
            return ResponseEntity.ok(Step5CompareResDto.error("세션에 PassValueDto가 없습니다."));
        }

        final String flow = dto.getFlow() == null ? "" : dto.getFlow().toUpperCase(Locale.ROOT);

        try {
            // 1) BEFORE
            List<CompareItemDto> before = new ArrayList<>();
            if ("RESERVE".equals(flow)) {
                Long userId = (Long) session.getAttribute("userId");
                if (userId == null) userId = 1001L;

                JsonNode req = om.createObjectNode().put("userId", userId);
                JsonNode res = tcp.sendMessage(new TcpMessage(Command.BUY_PLAN_CURRENT_LIST_GET, req));

                // TcpMessageHandler가 Object를 리턴 → 서버가 { data: <Object> }로 감싼다고 가정
                JsonNode data = res.path("data");
                if (data.has("data")) data = data.get("data"); // (양쪽 래핑 케이스 대비)

                for (JsonNode it : data.path("items")) {
                    before.add(new CompareItemDto(
                            it.path("productId").asText(),
                            it.path("productName").asText(""),
                            it.path("type").asText("FUND"),
                            it.path("percent").isNumber() ? it.path("percent").asInt() : null
                    ));
                }
            } else {
                // CHANGE/MATURITY: target 하나를 BEFORE
                String targetId = dto.getTargetProdId();
                if (targetId != null) {
                    before.add(resolveFromSession(targetId, dto.getFileUrlList()));
                }
            }

            // 2) AFTER
            List<CompareItemDto> after = new ArrayList<>();
            if ("RESERVE".equals(flow)) {
                Map<String, CompareItemDto> map = new LinkedHashMap<>();
                for (CompareItemDto b : before) map.put(b.getProductId(), cloneItem(b));

                // 제외
                if (dto.getTargetProdIdList() != null) {
                    for (String tid : dto.getTargetProdIdList()) {
                        if (tid != null) map.remove(tid);
                    }
                }

                // 추가/교체 (신구 혼용 허용: Map<SourceProductDto,Integer> 또는 Map<String,Integer>)
                if (dto.getSourceProdIdList() != null) {
                    for (Map<?, Integer> m : dto.getSourceProdIdList()) {
                        if (m == null) continue;
                        for (Map.Entry<?, Integer> e : m.entrySet()) {
                            String pid;
                            String pname = null;
                            Object key = e.getKey();
                            if (key instanceof SourceProductDto sp) {
                                pid = sp.getProductId();
                                pname = sp.getProductName();
                            } else {
                                pid = String.valueOf(key);
                            }
                            if (pid == null) continue;

                            int percent = e.getValue() == null ? 0 : e.getValue();
                            CompareItemDto it = new CompareItemDto(
                                    pid,
                                    pname == null ? guessName(pid, dto.getFileUrlList()) : pname,
                                    typeOf(pid),
                                    percent
                            );
                            map.put(pid, it); // upsert
                        }
                    }
                }
                after.addAll(map.values());

            } else {
                // CHANGE/MATURITY: AFTER는 source 하나
                String sid = dto.getSourceProdId();
                if (sid != null) {
                    after.add(resolveFromSession(sid, dto.getFileUrlList()));
                }
            }

            Step5CompareResDto body = new Step5CompareResDto(
                    flow, before, after, null, dto.getRiskGrade(), dto.getRiskGradeNum()
            );
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            return ResponseEntity.ok(Step5CompareResDto.error("비교 데이터 생성 실패: " + e.getMessage()));
        }
    }

    private static String typeOf(String productId) {
        return (productId != null && productId.startsWith("PG")) ? "PG" : "FUND";
    }

    private static String guessName(String productId, List<FileUrlDto> files) {
        if (files != null) {
            for (FileUrlDto f : files) {
                if (productId.equals(f.getProdId()) && f.getProdName() != null) {
                    return f.getProdName();
                }
            }
        }
        return productId;
    }

    private static CompareItemDto resolveFromSession(String productId, List<FileUrlDto> files) {
        return new CompareItemDto(productId, guessName(productId, files), typeOf(productId), null);
    }

    private static CompareItemDto cloneItem(CompareItemDto b) {
        return new CompareItemDto(b.getProductId(), b.getProductName(), b.getType(), b.getPercent());
    }
}
