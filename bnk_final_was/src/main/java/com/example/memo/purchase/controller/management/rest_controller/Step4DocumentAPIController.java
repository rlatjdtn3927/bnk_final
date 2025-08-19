// WAS - src/main/java/com/example/memo/purchase/controller/management/rest_controller/Step4DocumentAPIController.java
package com.example.memo.purchase.controller.management.rest_controller;

import com.example.memo.purchase.dto.trade.FileUrlDto;
import com.example.memo.purchase.dto.trade.PassValueDto;
import com.example.memo.purchase.dto.trade.Step4DocsRes;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/purchase/api/documents")
@RequiredArgsConstructor
public class Step4DocumentAPIController {

    @GetMapping("/step4")
    public ResponseEntity<Step4DocsRes> getDocsForStep4(HttpSession session) {
        PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
        if (dto == null) {
            return ResponseEntity.ok(Step4DocsRes.builder()
                    .message("세션 정보가 없습니다. 처음부터 다시 진행해주세요.")
                    .build());
        }

        final String flow = dto.getFlow();
        final List<String> prodIds = new ArrayList<>();
        final Map<String, Integer> ratios = new LinkedHashMap<>();

        if ("RESERVE".equalsIgnoreCase(flow)) {
            if (dto.getSourceProdIdList() != null) {
                for (Map<String, Integer> m : dto.getSourceProdIdList()) {
                    if (m == null) continue;
                    for (Map.Entry<String, Integer> e : m.entrySet()) {
                        final String pid = e.getKey();
                        if (!ratios.containsKey(pid)) {
                            ratios.put(pid, e.getValue());
                            prodIds.add(pid);
                        }
                    }
                }
            }
        } else {
            final String pid = dto.getSourceProdId();
            if (pid != null && !pid.isBlank()) prodIds.add(pid);
        }

        // 세션에 들어있는 fileUrlList에서 선택 상품만 필터
        List<FileUrlDto> all = dto.getFileUrlList() != null ? dto.getFileUrlList() : Collections.emptyList();
        List<FileUrlDto> filtered = all.stream()
                .filter(f -> f.getProdId() != null && prodIds.contains(f.getProdId()))
                .collect(Collectors.toList());

        // (선택) 세션도 최신 상태로 정리해둠
        dto.setFileUrlList(filtered);
        session.setAttribute("PassValueDto", dto);

        return ResponseEntity.ok(Step4DocsRes.builder()
                .flow(flow)
                .docs(filtered)
                .ratios(ratios.isEmpty() ? null : ratios)
                .message(filtered.isEmpty() ? "선택한 상품의 서류가 없습니다." : null)
                .build());
    }
}
