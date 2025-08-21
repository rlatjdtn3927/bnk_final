package com.example.memo.rule.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /**
     * 설문 제출 (POST)
     */
    @PostMapping("/survey-submit-view")
    public String submitSurvey(@RequestBody SurveySubmitReq req,
                               @SessionAttribute(value = "LOGIN_USER_ID", required = false) Long userId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (userId == null) {
            return "redirect:/login-view"; // 로그인 없으면 로그인 페이지로
        }

        try {
            // 1) userId 메타에 추가
            req.getMeta().put("userId", userId);

            // 2) TCP 메시지 생성
            JsonNode data = objectMapper.valueToTree(req);
            TcpMessage msg = new TcpMessage(Command.SURVEY_SUBMIT, data);

            // 3) AP 서버에 전송 후 응답 수신
            JsonNode responseNode = tcpClientService.sendMessage(msg);

            if (responseNode == null || responseNode.isNull()) {
                session.setAttribute("error", "AP 응답이 없습니다.");
                return "redirect:/survey-result";
            }

            // === AP 응답 로그 출력 ===
            System.out.println("[SurveyResultController] AP 응답 원본: " + responseNode.toPrettyString());

            // 4) 응답 구조 파싱
            JsonNode innerData = responseNode.get("data");
            if (innerData != null && innerData.has("data")) {
                innerData = innerData.get("data");
            }

            if (innerData == null || innerData.isNull()) {
                session.setAttribute("error", "응답 파싱 실패: data 필드 없음");
                return "redirect:/survey-result";
            }

            // 5) SurveySubmitRes 변환
            SurveySubmitRes result = objectMapper.convertValue(innerData, SurveySubmitRes.class);

            // 6) 세션에 결과 저장 (Flash 대신)
            session.setAttribute("result", result);

            return "redirect:/survey-result"; // ✅ GET으로 리다이렉트
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("error", "설문 처리 오류: " + e.getMessage());
            return "redirect:/survey-result";
        }
    }

    /**
     * 설문 결과 페이지 (GET)
     */
    @GetMapping("/survey-result")
    public String showResultPage(HttpSession session, Model model) {
        SurveySubmitRes result = (SurveySubmitRes) session.getAttribute("result");
        String error = (String) session.getAttribute("error");

        if (result != null) {
            model.addAttribute("score", result.getScore());
            model.addAttribute("type", result.getType());
        } else {
            model.addAttribute("score", 0);
            model.addAttribute("type", "-");
        }
        model.addAttribute("error", error);

        System.out.println(">>> score=" + model.getAttribute("score"));
        System.out.println(">>> type=" + model.getAttribute("type"));
        System.out.println(">>> error=" + error);

        return "rule/survey-result";
    }
}
