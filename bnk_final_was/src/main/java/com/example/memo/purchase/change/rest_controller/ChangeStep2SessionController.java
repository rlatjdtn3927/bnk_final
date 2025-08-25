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

        System.out.println("== [/sold/save] 요청 매도아이템 " + items.size() + "건");
        for (ChangeValueDto.SoldItem item : items) {
            if ("FUND".equalsIgnoreCase(item.getType())) {
                System.out.println("   [FUND-REQ] prodId=" + item.getProdId()
                        + ", holdingId=" + item.getHoldingId()
                        + ", ratio=" + item.getRatio()
                        + ", amount=" + item.getAmount());

                if (item.getHoldingId() == null || item.getHoldingId().isBlank()) {
                    item.setHoldingId(item.getProdId());
                    System.out.println("   [보정] FUND holdingId -> prodId 사용: " + item.getProdId());
                }
                if (item.getProdId() == null || item.getProdId().isBlank()) {
                    System.out.println("⚠️ [WARN] FUND prodId 누락됨! holdingId=" + item.getHoldingId());
                }
            }
            else if ("PRINCIPAL".equalsIgnoreCase(item.getType())) {
                System.out.println("   [PRINCIPAL-REQ] ledgerId=" + item.getLedgerId()
                        + ", id=" + item.getId()
                        + ", prodId=" + item.getProdId()
                        + ", ratio=" + item.getRatio()
                        + ", amount=" + item.getAmount());

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
            else {
                System.out.println("   [UNKNOWN TYPE] " + item);
            }
        }

        BigDecimal total = items.stream()
            .map(i -> i.getAmount() == null ? BigDecimal.ZERO : i.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setSoldItems(items);
        dto.setSoldTotalAmount(total);
        session.setAttribute("ChangeValueDto", dto);

        System.out.println("== [/sold/save] 세션 저장 완료, 총액=" + total);

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

        System.out.println("== [/buy/save] 요청 body: " + body);

        if (body instanceof List) {
            items = mapper.convertValue(body,
                mapper.getTypeFactory().constructCollectionType(List.class, ChangeValueDto.BuyItem.class));
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

        // FUND / PRINCIPAL 나눠서 로그 출력
        for (ChangeValueDto.BuyItem it : items) {
            if ("FUND".equalsIgnoreCase(it.getType())) {
                System.out.println("   [FUND-BUY] prodId=" + it.getProdId()
                        + ", name=" + it.getName()
                        + ", ratio=" + it.getRatio()
                        + ", amount=" + it.getAmount());
            } else if ("PRINCIPAL".equalsIgnoreCase(it.getType())) {
                System.out.println("   [PRINCIPAL-BUY] prodId=" + it.getProdId()
                        + ", name=" + it.getName()
                        + ", ratio=" + it.getRatio()
                        + ", amount=" + it.getAmount());
            } else {
                System.out.println("   [UNKNOWN-BUY] " + it);
            }
        }

        BigDecimal total = items.stream()
            .map(i -> i.getAmount() == null ? BigDecimal.ZERO : i.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setBuyItems(items);
        dto.setBuyTotalAmount(total);

        try { dto.setFileUrlList(mapper.writeValueAsString(fileUrls)); }
        catch (Exception e) {
            if (dto.getFileUrlList() == null) dto.setFileUrlList("[]");
        }

        session.setAttribute("ChangeValueDto", dto);

        System.out.println("== [/buy/save] 세션 저장 완료, 총액=" + total);

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
        System.out.println("== [/snapshot] 세션 DTO: " + dto);
        return ResponseEntity.ok(Map.of("dto", dto));
    }
}
