package com.example.memo.purchase.change.rest_controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.memo.purchase.trade_common.dto.ChangeValueDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/retain-api/change/step2")
@RequiredArgsConstructor
public class ChangeStep2SessionController {

    @PostMapping("/sold/save")
    public ResponseEntity<?> saveSold(@RequestBody List<ChangeValueDto.SoldItem> items, HttpSession session) {
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null) dto = new ChangeValueDto();

        BigDecimal total = items.stream()
            .map(i -> i.getAmount() == null ? BigDecimal.ZERO : i.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setSoldItems(items);
        dto.setSoldTotalAmount(total);
        session.setAttribute("ChangeValueDto", dto);
        return ResponseEntity.ok(Map.of("ok", true, "soldTotal", total));
    }

    @PostMapping("/buy/save")
    public ResponseEntity<?> saveBuy(@RequestBody List<ChangeValueDto.BuyItem> items, HttpSession session) {
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null) dto = new ChangeValueDto();

        BigDecimal total = items.stream()
            .map(i -> i.getAmount() == null ? BigDecimal.ZERO : i.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setBuyItems(items);
        dto.setBuyTotalAmount(total);
        session.setAttribute("ChangeValueDto", dto);
        return ResponseEntity.ok(Map.of("ok", true, "buyTotal", total));
    }

    @GetMapping("/snapshot")
    public ResponseEntity<?> snapshot(HttpSession session) {
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null) dto = new ChangeValueDto(); // ✅ NPE 방지
        return ResponseEntity.ok(Map.of("dto", dto));
    }
}
