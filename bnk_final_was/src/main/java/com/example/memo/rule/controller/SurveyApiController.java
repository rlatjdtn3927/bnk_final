package com.example.memo.rule.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.example.memo.rule.dto.SurveySubmitReq;
import com.example.memo.rule.dto.SurveySubmitRes;
import com.example.memo.tcp_common.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;

@RequiredArgsConstructor
@RestController
@RequestMapping("/survey-api")
public class SurveyApiController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper objectMapper;

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody SurveySubmitReq req, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorBody("UNAUTHORIZED", "로그인이 필요합니다."));
        }

        try {
            // ✅ meta null-safe
            if (req.getMeta() == null) {
                req.setMeta(new HashMap<>());
            }
            req.getMeta().put("userId", userId);

            // 요청 전송
            JsonNode data = objectMapper.valueToTree(req);
            TcpMessage msg = new TcpMessage(Command.SURVEY_SUBMIT, data);
            JsonNode responseNode = tcpClientService.sendMessage(msg);

            if (responseNode == null || responseNode.isNull()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorBody("NO_RESPONSE", "AP 응답이 없습니다."));
            }

            // === 디버그 로그 ===
            System.out.println("=== SurveyApiController AP 응답 ===");
            System.out.println(responseNode.toPrettyString());

            // 1) encData 꺼내기
            String encData = responseNode.path("encData").asText(null);
            if (encData == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ErrorBody("BAD_AP_RESPONSE", "AP 응답에 encData 없음"));
            }

            // 2) AES256 복호화
            String plainJson = AES256Util.decrypt(encData);
            System.out.println(">>> 복호화된 평문: " + plainJson);

            // 3) 평문 → JsonNode
            JsonNode plainNode = objectMapper.readTree(plainJson);

            // 4) success 여부 체크
            if (!plainNode.path("success").asBoolean(false)) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ErrorBody("AP_FAIL", "AP 처리 실패: " + plainNode));
            }

            // 5) data → DTO 매핑
            SurveySubmitRes result = objectMapper.convertValue(plainNode.path("data"), SurveySubmitRes.class);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorBody("ERROR", "설문 제출 처리 중 오류 발생: " + e.getMessage()));
        }
    }

    record ErrorBody(String code, String message) {}
}
