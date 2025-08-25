// src/main/java/com/example/memo/purchase/change/rest_controller/ChangeDocsController.java
package com.example.memo.purchase.change.rest_controller;

import com.example.memo.purchase.trade_common.dto.ChangeValueDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/retain-api/change")
@RequiredArgsConstructor
public class ChangeDocsController {

    private final ObjectMapper mapper = new ObjectMapper();

    /** ✅ Step4 문서 목록: Step2에서 저장한 fileUrlList 그대로 반환 */
    @GetMapping("/docs/list")
    public ResponseEntity<?> docsList(HttpSession session) {
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null || dto.getBuyItems() == null) {
            return ResponseEntity.ok(Map.of("files", List.of()));
        }

        List<Map<String, Object>> files = new ArrayList<>();
        try {
            // fileUrlList → 배열로 변환
            List<Map<String, Object>> savedFiles = new ArrayList<>();
            if (dto.getFileUrlList() != null) {
                savedFiles = mapper.readValue(
                    dto.getFileUrlList(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>() {}
                );
            }

            // buyItems 기준으로 돌면서, fileUrlList 에 있는 항목 매핑
            for (ChangeValueDto.BuyItem item : dto.getBuyItems()) {
                if (item == null || item.getProdId() == null) continue;

                // fileUrlList 에서 해당 prodId 관련 문서만 추출
                for (Map<String,Object> f : savedFiles) {
                    String pid = (String) f.get("prodId");
                    if (pid != null && pid.equals(item.getProdId())) {
                        files.add(f);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(Map.of("files", files));
    }

}
