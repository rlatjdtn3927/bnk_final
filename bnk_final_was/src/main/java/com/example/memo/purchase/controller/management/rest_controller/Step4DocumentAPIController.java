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
	public ResponseEntity<Map<String, Object>> getDocsForStep4(HttpSession session) {
	    PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
	    if (dto == null) {
	        // 여기서는 message가 null이 아니므로 Map.of 사용 가능
	        return ResponseEntity.ok(Map.of(
	            "flow", null,
	            "docs", List.of(),
	            "ratios", Map.of(),
	            "message", "세션에 PassValueDto가 없습니다."
	        ));
	    }

	    final String flow = dto.getFlow() == null ? "" : dto.getFlow().toUpperCase(Locale.ROOT);

	    // prodIds/ratios 계산(생략: 기존 코드 그대로)
	    final List<String> prodIds = new ArrayList<>();
	    final Map<String, Integer> ratios = new LinkedHashMap<>();
	    if ("RESERVE".equals(flow) && dto.getSourceProdIdList() != null) {
	        for (Map<SourceProductDto, Integer> m : dto.getSourceProdIdList()) {
	            if (m == null) continue;
	            for (Map.Entry<SourceProductDto, Integer> e : m.entrySet()) {
	                SourceProductDto key = e.getKey();
	                if (key == null || key.getProductId() == null) continue;
	                String pid = key.getProductId();
	                if (!ratios.containsKey(pid)) {
	                    ratios.put(pid, e.getValue() == null ? 0 : e.getValue());
	                    prodIds.add(pid);
	                }
	            }
	        }
	    } else if (dto.getSourceProdId() != null) {
	        prodIds.add(dto.getSourceProdId());
	    }

	    // fileUrlList 필터링
	    final List<FileUrlDto> docs = new ArrayList<>();
	    if (dto.getFileUrlList() != null) {
	        for (FileUrlDto f : dto.getFileUrlList()) {
	            if (f != null && prodIds.contains(f.getProdId())) {
	                docs.add(f);
	            }
	        }
	    }

	    // ✅ null 값 넣지 않도록 조립
	    Map<String, Object> body = new LinkedHashMap<>();
	    body.put("flow", flow);
	    body.put("docs", docs);
	    body.put("ratios", ratios);
	    if (docs.isEmpty()) {
	        body.put("message", "표시할 문서가 없습니다.");
	    }
	    return ResponseEntity.ok(body);
	}
}
