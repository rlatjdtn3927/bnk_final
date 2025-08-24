// src/main/java/com/example/memo/purchase/change/rest_controller/ChangeStep2SessionController.java
package com.example.memo.purchase.change.rest_controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.memo.purchase.trade_common.dto.ChangeValueDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/retain-api/change/step2")
@RequiredArgsConstructor
public class ChangeStep2SessionController {

    private final ObjectMapper mapper = new ObjectMapper();

    /** ✅ 매도 저장 */
    @PostMapping("/sold/save")
    public ResponseEntity<?> saveSold(@RequestBody List<ChangeValueDto.SoldItem> items, HttpSession session) {
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null) dto = new ChangeValueDto();

        // ✅ PK 보강
        for (ChangeValueDto.SoldItem item : items) {
            if ("FUND".equalsIgnoreCase(item.getType())) {
                // holdingId 보정
                if (item.getHoldingId() == null || item.getHoldingId().isBlank()) {
                    item.setHoldingId(item.getProdId()); // fallback
                    System.out.println("== [보정] FUND holdingId -> prodId 사용: " + item.getProdId());
                }
                // prodId 누락 확인
                if (item.getProdId() == null || item.getProdId().isBlank()) {
                    System.out.println("⚠️ [WARN] FUND prodId 누락됨! holdingId=" + item.getHoldingId());
                }
            }

            if ("PRINCIPAL".equalsIgnoreCase(item.getType())) {
                // ledgerId <-> id 싱크
                if (item.getLedgerId() == null && item.getId() != null) {
                    item.setLedgerId(item.getId());
                }
                if (item.getId() == null && item.getLedgerId() != null) {
                    item.setId(item.getLedgerId());
                }

                if (item.getLedgerId() == null) {
                    System.out.println("⚠️ [WARN] PRINCIPAL ledgerId 누락됨!");
                }
            }
        }

        // ✅ 매도 총액 계산
        BigDecimal total = items.stream()
            .map(i -> i.getAmount() == null ? BigDecimal.ZERO : i.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setSoldItems(items);
        dto.setSoldTotalAmount(total);
        session.setAttribute("ChangeValueDto", dto);

        return ResponseEntity.ok(Map.of(
            "ok", true,
            "soldTotal", total,
            "soldItems", items
        ));
    }

    /** ✅ 매수 저장 */
    @PostMapping("/buy/save")
    public ResponseEntity<?> saveBuy(@RequestBody Object body, HttpSession session) {
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null) dto = new ChangeValueDto();

        List<ChangeValueDto.BuyItem> items = new ArrayList<>();
        List<Map<String,Object>> fileUrls = new ArrayList<>();

        if (body instanceof List) {
            // JS에서 배열만 보낸 경우
            items = mapper.convertValue(body,
                mapper.getTypeFactory().constructCollectionType(List.class, ChangeValueDto.BuyItem.class));

            // fileUrlList 유지
            if (dto.getFileUrlList() != null) {
                try { fileUrls = mapper.readValue(dto.getFileUrlList(), List.class); }
                catch (Exception ignored) {}
            }
        } else if (body instanceof Map) {
            Map<String,Object> map = (Map<String,Object>) body;
            items = mapper.convertValue(map.get("buyItems"),
                mapper.getTypeFactory().constructCollectionType(List.class, ChangeValueDto.BuyItem.class));
            fileUrls = mapper.convertValue(map.get("fileUrlList"), List.class);
        }

        BigDecimal total = items.stream()
            .map(i -> i.getAmount() == null ? BigDecimal.ZERO : i.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setBuyItems(items);
        dto.setBuyTotalAmount(total);

        // fileUrlList 저장 (없으면 기존 유지)
        try { dto.setFileUrlList(mapper.writeValueAsString(fileUrls)); }
        catch (Exception e) {
            if (dto.getFileUrlList() == null) dto.setFileUrlList("[]");
        }

        session.setAttribute("ChangeValueDto", dto);

        return ResponseEntity.ok(Map.of(
            "ok", true,
            "buyTotal", total,
            "buyItems", items,
            "fileUrls", fileUrls
        ));
    }

    /** ✅ 세션 스냅샷 */
    @GetMapping("/snapshot")
    public ResponseEntity<?> snapshot(HttpSession session) {
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null) dto = new ChangeValueDto();
        return ResponseEntity.ok(Map.of("dto", dto));
    }
}
