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
        if (dto == null) return ResponseEntity.ok(Map.of("files", List.of()));

        List<Map<String,Object>> files;
        try {
            files = mapper.readValue(dto.getFileUrlList(), List.class);
        } catch (Exception e) {
            files = List.of();
        }

        return ResponseEntity.ok(Map.of("files", files));
    }
}
