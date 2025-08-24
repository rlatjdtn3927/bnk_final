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
    private final AES256Util AES256Util;

    @PostMapping("/survey-api/submit")
    public ResponseEntity<?> submit(@RequestBody SurveySubmitReq req, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorBody("UNAUTHORIZED", "로그인이 필요합니다."));
        }
        try {
            if (req.getMeta() == null) req.setMeta(new HashMap<>());
            req.getMeta().put("userId", userId);

            JsonNode data = objectMapper.valueToTree(req);
            TcpMessage msg = new TcpMessage(Command.SURVEY_SUBMIT, data);
            JsonNode resp = tcpClientService.sendMessage(msg);
            if (resp == null || resp.isNull()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorBody("NO_RESPONSE", "AP 응답이 없습니다."));
            }

            // 🔧 encData는 resp.data.encData 에 있습니다.
            String encData = resp.path("data").path("encData").asText(null);
            if (encData == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ErrorBody("BAD_AP_RESPONSE", "AP 응답에 encData 없음"));
            }

            String plainJson = AES256Util.decrypt(encData);
            System.out.println(">>> 복호화 평문: " + plainJson);
            JsonNode plain = objectMapper.readTree(plainJson);

            if (!plain.path("success").asBoolean(false)) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ErrorBody("AP_FAIL", "AP 처리 실패: " + plain));
            }

            SurveySubmitRes result = objectMapper.convertValue(plain.path("data"), SurveySubmitRes.class);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorBody("ERROR", "설문 제출 처리 중 오류: " + e.getMessage()));
        }
    }


    record ErrorBody(String code, String message) {}
}
