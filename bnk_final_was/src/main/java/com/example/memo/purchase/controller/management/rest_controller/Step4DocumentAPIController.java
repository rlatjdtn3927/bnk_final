// WAS - src/main/java/com/example/memo/purchase/controller/management/rest_controller/Step4DocumentAPIController.java
package com.example.memo.purchase.controller.management.rest_controller;

import com.example.memo.purchase.dto.trade.FileUrlDto;
import com.example.memo.purchase.dto.trade.PassValueDto;
import com.example.memo.purchase.dto.trade.SourceProductDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/purchase/api/documents")
@RequiredArgsConstructor
public class Step4DocumentAPIController {

    @GetMapping("/step4")
    public ResponseEntity<Map<String,Object>> getDocsForStep4(HttpSession session) {
        PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
        if (dto == null) {
            return ok(Map.of(
                    "flow", null,
                    "docs", List.of(),
                    "ratios", Map.of(),
                    "message", "세션에 PassValueDto가 없습니다."
            ));
        }

        final String flow = (dto.getFlow() == null) ? "" : dto.getFlow().toUpperCase(Locale.ROOT);

        // 1) 선택된 prodId 목록 + RESERVE라면 비율맵
        final List<String> prodIds = new ArrayList<>();
        final Map<String, Integer> ratios = new LinkedHashMap<>();

        if ("RESERVE".equals(flow)) {
            if (dto.getSourceProdIdList() != null) {
                for (Map<SourceProductDto, Integer> m : dto.getSourceProdIdList()) {
                    if (m == null) continue;
                    for (Map.Entry<SourceProductDto, Integer> e : m.entrySet()) {
                        SourceProductDto key = e.getKey();
                        if (key == null || key.getProductId() == null) continue;
                        final String pid = key.getProductId();
                        if (!ratios.containsKey(pid)) {
                            ratios.put(pid, e.getValue() == null ? 0 : e.getValue());
                            prodIds.add(pid);
                        }
                    }
                }
            }
        } else {
            if (dto.getSourceProdId() != null) {
                prodIds.add(dto.getSourceProdId());
            }
        }

        // 2) 세션 fileUrlList에서 해당 prodId만 필터
        final List<FileUrlDto> docs = new ArrayList<>();
        if (dto.getFileUrlList() != null) {
            for (FileUrlDto f : dto.getFileUrlList()) {
                if (f != null && prodIds.contains(f.getProdId())) {
                    docs.add(f);
                }
            }
        }

        return ok(Map.of(
                "flow", flow,
                "docs", docs,      // 그대로 배열(JSON)로 내려감
                "ratios", ratios,  // RESERVE만 의미, 나머지는 빈맵
                "message", (docs.isEmpty() ? "표시할 문서가 없습니다." : null)
        ));
    }

    private ResponseEntity<Map<String,Object>> ok(Map<String,Object> body){
        return ResponseEntity.ok(body);
    }
}
