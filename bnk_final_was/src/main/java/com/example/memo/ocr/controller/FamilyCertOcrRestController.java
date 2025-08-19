package com.example.memo.ocr.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.ocr.dto.FamilyCertOcrRequest;
import com.example.memo.ocr.dto.FamilyCertOcrResponse;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyCertOcrRestController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper objectMapper;

    @Value("${family.ocr.template-id}")
    private String templateId;

    @PostMapping("ocr")
    public ResponseEntity<?> ocr(@RequestBody FamilyCertOcrRequest reqBody) {
        try {
            // 1) 템플릿ID는 서버 설정값 사용
            String templateId = this.templateId; // @Value 주입된 값

            // 2) AP로 보낼 data JSON 만들기
            ObjectNode data = objectMapper.createObjectNode();
            data.put("base64Data", reqBody.base64Data());
            data.put("format", (reqBody.format() == null || reqBody.format().isBlank()) ? "pdf" : reqBody.format());
            data.put("expectedApplicantName", reqBody.expectedApplicantName());
            data.put("templateId", templateId);

            // 3) TcpMessage 구성 후 전송
            TcpMessage message = new TcpMessage(Command.FAMILY_CERT_OCR, data);
            JsonNode apResponse = tcpClientService.sendMessage(message);  // ★ 여기!

            if (apResponse == null) {
                return ResponseEntity.badRequest().body(
                    Map.of("verified", false, "reason", "AP 응답이 null입니다.")
                );
            }

            // 4) 응답 매핑 후 그대로 리턴
            FamilyCertOcrResponse res =
                objectMapper.convertValue(apResponse, FamilyCertOcrResponse.class);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                Map.of("verified", false, "reason", "OCR 호출 실패: " + e.getMessage())
            );
        }
    }
}