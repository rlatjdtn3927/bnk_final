// src/main/java/com/example/memo/purchase/reserve/rest_controller/ReserveApplyAPIController.java
package com.example.memo.purchase.reserve.rest_controller;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.memo.purchase.reserve.dto.SourceProdRatioDto;
import com.example.memo.purchase.trade_common.dto.ReserveValueDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/purchase/api/trade")
@RequiredArgsConstructor
public class ReserveApplyAPIController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    /**
     * RESERVE 적용 API
     * step6.html의 "적용" 버튼에서 호출
     * 세션의 ReserveValueDto → BUY_PLAN_UPDATE 로 AP 전달
     */
    @PostMapping("/reserve/apply")
    public ResponseEntity<?> applyReserve(HttpSession session) {
        System.out.println("\n==================== [RESERVE APPLY] 호출 ====================");

        // 0) 세션 덤프 (키/값 존재 여부 확인)
        System.out.println(">> 세션 덤프 (attribute names)");
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String k = names.nextElement();
            Object v = session.getAttribute(k);
            System.out.println("   - " + k + " = " + (v == null ? "null" : v.getClass().getSimpleName()));
        }

        // 1) DTO 존재 확인
        ReserveValueDto v = (ReserveValueDto) session.getAttribute("ReserveValueDto");
        if (v == null) {
            System.out.println("!! ReserveValueDto 없음");
            return ResponseEntity.ok(err("세션에 ReserveValueDto가 없습니다."));
        }
        try {
            // DTO 내용 전체 출력
            System.out.println(">> ReserveValueDto(JSON) = " + om.writerWithDefaultPrettyPrinter().writeValueAsString(v));
        } catch (Exception ignore) {}

        // 2) userId 확인 (키 다양화)
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) userId = (Long) session.getAttribute("userId");
        if (userId == null) userId = (Long) session.getAttribute("user_id");
        System.out.println(">> userId (resolved) = " + userId);
        if (userId == null) return ResponseEntity.ok(err("세션에 사용자 ID가 없습니다."));

        try {
            // 3) 원시 문자열 그대로 확인
            String rawTargets = v.getTargetProdIdList();
            String rawSources = v.getSourceProdIdList();
            System.out.println(">> raw targetProdIdList = " + rawTargets);
            System.out.println(">> raw sourceProdIdList = " + rawSources);

            // 4) 문자열(JSON) → JsonNode 파싱
            JsonNode targetNode = parseNode(rawTargets);
            JsonNode sourceNode = parseNode(rawSources);

            System.out.println(">> parsed targetNode = " + targetNode.toPrettyString());
            System.out.println(">> parsed sourceNode = " + sourceNode.toPrettyString());

            // 5) List 변환
            List<String> targetIds = toStringList(targetNode);
            List<SourceProdRatioDto> sourceList = toSourceList(sourceNode);

            System.out.println(">> targetIds = " + targetIds);
            System.out.println(">> sourceList.size = " + sourceList.size());
            for (int i = 0; i < sourceList.size(); i++) {
                SourceProdRatioDto s = sourceList.get(i);
                System.out.println(String.format("   [%d] prodId=%s, ratio=%s", i, s.getProdId(), s.getRatio()));
            }

            if (sourceList.isEmpty()) {
                System.out.println("!! sourceList 가 비었습니다. 프론트/세션 JSON을 확인하세요.");
            }

            // 6) AP payload 구성
            ObjectNode payload = om.createObjectNode();
            payload.put("userId", userId);
            payload.put("accountType", v.getAccountType()); // IRP or DC
            payload.put("accountId", v.getAccountId());

            ArrayNode targetArr = om.createArrayNode();
            for (String id : targetIds) targetArr.add(id);
            payload.set("targetProdIdList", targetArr);

            ArrayNode sourceArr = om.createArrayNode();
            for (SourceProdRatioDto s : sourceList) {
                if (s.getProdId() == null) continue;
                ObjectNode item = om.createObjectNode();
                item.put("prodId", s.getProdId());
                if (s.getRatio() != null) item.put("ratio", s.getRatio());
                sourceArr.add(item);
            }
            payload.set("sourceProdList", sourceArr);

            System.out.println(">> BUY_PLAN_UPDATE payload = " + payload.toPrettyString());

            // 7) TCP 호출
            TcpMessage msg = new TcpMessage(Command.BUY_PLAN_UPDATE, payload);
            System.out.println(">> Command = " + msg.getCommand());

            JsonNode apResp = tcp.sendMessage(msg);
            System.out.println(">> AP response = " + (apResp == null ? "null" : apResp.toPrettyString()));

            // 8) 응답
            ObjectNode ok = om.createObjectNode();
            ok.put("success", true);
            ok.set("apResponse", apResp == null ? om.nullNode() : apResp);
            System.out.println("==================== [RESERVE APPLY] 종료 ====================\n");
            return ResponseEntity.ok(ok);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("==================== [RESERVE APPLY] 예외 종료 ====================\n");
            return ResponseEntity.ok(err("적용 중 오류: " + e.getMessage()));
        }
    }

    // ---- Helper 메서드 ----

    /** 문자열이 JSON이면 readTree, 이중 인코딩(문자열 안에 JSON)도 1회 방어 */
    private JsonNode parseNode(String json) {
        try {
            if (json == null || json.isBlank()) return om.createArrayNode();
            JsonNode n = om.readTree(json);
            if (n.isTextual()) { // 상위가 통째로 문자열인 경우
                try { return om.readTree(n.asText()); } catch (Exception ignore) {}
            }
            return n;
        } catch (Exception e) {
            return om.createArrayNode();
        }
    }

    /** ["a","b"] → List<String> */
    private List<String> toStringList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> { if (n.isTextual()) out.add(n.asText()); });
        }
        return out;
    }

    /**
     * [{product:{productId,...}, ratio:20}, ...] → List<SourceProdRatioDto>
     * - prodId는 product.productId 또는 prodId 중 우선순위로 찾음
     */
    private List<SourceProdRatioDto> toSourceList(JsonNode node) {
        List<SourceProdRatioDto> out = new ArrayList<>();
        if (node == null || !node.isArray()) return out;

        node.forEach(n -> {
            String pid = n.path("product").path("productId").asText(null);
            if (pid == null || pid.isBlank()) pid = n.path("prodId").asText(null);

            Integer ratio = n.hasNonNull("ratio") && n.get("ratio").canConvertToInt()
                    ? n.get("ratio").asInt()
                    : null;

            if (pid != null)
                out.add(SourceProdRatioDto.builder().prodId(pid).ratio(ratio).build());
        });
        return out;
    }

    /** 에러 응답 */
    private ObjectNode err(String msg) {
        ObjectNode o = om.createObjectNode();
        o.put("success", false);
        o.put("message", msg);
        return o;
    }
}
