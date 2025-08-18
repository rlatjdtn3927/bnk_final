package com.example.memo.rule.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.example.memo.rule.dto.SurveySubmitReq;
import com.example.memo.rule.dto.SurveySubmitRes;
import com.example.memo.tcp_common.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/survey-api")
public class SurveyApiController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper objectMapper;

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody SurveySubmitReq req, HttpSession session) {
        Long userId = (Long) session.getAttribute("user");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorBody("UNAUTHORIZED", "로그인이 필요합니다."));
        }

        try {
            req.getMeta().put("userId", userId);

            JsonNode data = objectMapper.valueToTree(req);
            TcpMessage msg = new TcpMessage(Command.SURVEY_SUBMIT, data);
            JsonNode responseNode = tcpClientService.sendMessage(msg);

            if (responseNode == null || responseNode.isNull()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorBody("NO_RESPONSE", "AP 응답이 없습니다."));
            }

            // === 디버그 로그 ===
            System.out.println("=== SurveyApiController AP 응답 ===");
            try {
                System.out.println(responseNode.toPrettyString());
            } catch (Exception ex) {
                System.out.println(responseNode.toString());
            }

            // data → data.data 안전하게 꺼내기
            JsonNode innerData = responseNode.path("data");
            if (innerData.has("data")) {
                innerData = innerData.get("data");
            }

            if (innerData == null || innerData.isNull()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ErrorBody("BAD_AP_RESPONSE", "AP 응답의 data가 비어있습니다."));
            }

            SurveySubmitRes result = objectMapper.convertValue(innerData, SurveySubmitRes.class);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorBody("ERROR", "설문 제출 처리 중 오류 발생: " + e.getMessage()));
        }
    }


    record ErrorBody(String code, String message) {}
}
