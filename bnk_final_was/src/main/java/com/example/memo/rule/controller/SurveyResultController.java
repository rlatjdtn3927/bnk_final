package com.example.memo.rule.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.example.memo.rule.dto.SurveySubmitReq;
import com.example.memo.rule.dto.SurveySubmitRes;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class SurveyResultController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper objectMapper;

    @PostMapping("/survey-submit-view")
    public String submitAndView(@RequestBody SurveySubmitReq req,
                                @SessionAttribute(value = "LOGIN_USER_ID", required = false) Long userId,
                                Model model) {

        try {
            // userId 추가
            req.getMeta().put("userId", userId);

            // TCP 메시지 생성
            JsonNode data = objectMapper.valueToTree(req);
            TcpMessage msg = new TcpMessage(Command.SURVEY_SUBMIT, data);

            // AP에 전송 후 응답 받기
            JsonNode responseNode = tcpClientService.sendMessage(msg);

            if (responseNode == null || responseNode.isNull()) {
                model.addAttribute("error", "AP 응답이 없습니다.");
                return "survey-result";
            }

            // === AP 응답 로그 출력 ===
            System.out.println("AP 응답 원본: " + responseNode.toPrettyString());

            // === 유연한 파싱 ===
            JsonNode innerData = responseNode.get("data");
            if (innerData != null && innerData.has("data")) {
                innerData = innerData.get("data"); // 중첩 구조 처리
            }

            SurveySubmitRes result = objectMapper.convertValue(innerData, SurveySubmitRes.class);

            // 결과를 모델에 담아 전달
            model.addAttribute("score", result.getScore());
            model.addAttribute("type", result.getType());

            return "survey-result"; // templates/survey-result.html
        } catch (Exception e) {
            model.addAttribute("error", "설문 처리 오류: " + e.getMessage());
            return "survey-result";
        }
    }
}
