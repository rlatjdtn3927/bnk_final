// src/main/java/com/example/memo/purchase/change/rest_controller/ChangeStep2SessionController.java
package com.example.memo.purchase.change.rest_controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.purchase.trade_common.dto.ChangeValueDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/retain-api/change/step2")
@RequiredArgsConstructor
public class ChangeStep2SessionController {

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/sold/save")
    public ResponseEntity<?> saveSold(@RequestBody List<ChangeValueDto.SoldItem> items, HttpSession session) {
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null) dto = new ChangeValueDto();

        BigDecimal total = items.stream()
            .map(i -> i.getAmount() == null ? BigDecimal.ZERO : i.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ✅ PrincipalLedgerDto.id를 반드시 저장하도록 보강
        for (ChangeValueDto.SoldItem item : items) {
            if ("PRINCIPAL".equalsIgnoreCase(item.getType())) {
                if (item.getLedgerId() == null) {
                    System.out.println("== [WARN] PRINCIPAL 매도인데 id 없음 → 오류 가능");
                } else {
                    System.out.println("== [INFO] PRINCIPAL 매도 저장 (ledger.id=" + item.getLedgerId() + ")");
                }
            }
        }

        dto.setSoldItems(items);
        dto.setSoldTotalAmount(total);
        session.setAttribute("ChangeValueDto", dto);

        return ResponseEntity.ok(Map.of("ok", true, "soldTotal", total, "soldItems", items));
    }

    /** ✅ 선택한 매수상품 + 문서까지 세션에 저장 */
    @PostMapping("/buy/save")
    public ResponseEntity<?> saveBuy(@RequestBody Object body, HttpSession session) {
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null) dto = new ChangeValueDto();

        List<ChangeValueDto.BuyItem> items = new ArrayList<>();
        List<Map<String,Object>> fileUrls = new ArrayList<>();

        if (body instanceof List) {
            // JS에서 배열만 보낸 경우: [ {...}, {...} ]
            items = mapper.convertValue(body,
                    mapper.getTypeFactory().constructCollectionType(List.class, ChangeValueDto.BuyItem.class));

            // ✅ fileUrlList는 덮어쓰지 말고 세션에 있던 값 유지
            if (dto.getFileUrlList() != null) {
                try {
                    fileUrls = mapper.readValue(dto.getFileUrlList(), List.class);
                } catch (Exception ignored) {}
            }
        } else if (body instanceof Map) {
            Map<String,Object> map = (Map<String,Object>) body;

            items = mapper.convertValue(map.get("buyItems"),
                    mapper.getTypeFactory().constructCollectionType(List.class, ChangeValueDto.BuyItem.class));

            fileUrls = mapper.convertValue(map.get("fileUrlList"), List.class);
        }

        // 합계 계산
        BigDecimal total = items.stream()
                .map(i -> i.getAmount() == null ? BigDecimal.ZERO : i.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setBuyItems(items);
        dto.setBuyTotalAmount(total);

        // fileUrlList 저장 (없으면 기존 값 유지)
        try {
            dto.setFileUrlList(mapper.writeValueAsString(fileUrls));
        } catch (Exception e) {
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


    @GetMapping("/snapshot")
    public ResponseEntity<?> snapshot(HttpSession session) {
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null) dto = new ChangeValueDto();
        return ResponseEntity.ok(Map.of("dto", dto));
    }
}
