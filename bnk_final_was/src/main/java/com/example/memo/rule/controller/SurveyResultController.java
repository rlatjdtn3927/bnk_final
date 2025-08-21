package com.example.memo.rule.controller;

import java.util.HashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.example.memo.rule.dto.SurveySubmitReq;
import com.example.memo.rule.dto.SurveySubmitRes;
import com.example.memo.tcp_common.AES256Util;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
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
                               HttpSession session) {
        if (userId == null) return "redirect:/login-view";
        try {
            if (req.getMeta() == null) req.setMeta(new HashMap<>());
            req.getMeta().put("userId", userId);

            JsonNode data = objectMapper.valueToTree(req);
            TcpMessage msg = new TcpMessage(Command.SURVEY_SUBMIT, data);
            JsonNode resp = tcpClientService.sendMessage(msg);

            if (resp == null || resp.isNull()) {
                session.setAttribute("error", "AP 응답이 없습니다.");
                return "redirect:/survey-result";
            }
            System.out.println("[SurveyResultController] AP 응답 원본: " + resp.toPrettyString());

            // 🔧 encData는 resp.data.encData
            String encData = resp.path("data").path("encData").asText(null);
            if (encData == null) {
                session.setAttribute("error", "응답 파싱 실패: encData 없음");
                return "redirect:/survey-result";
            }

            String plainJson = AES256Util.decrypt(encData);
            System.out.println("[SurveyResultController] 복호화 평문: " + plainJson);
            JsonNode plain = objectMapper.readTree(plainJson);

            if (!plain.path("success").asBoolean(false)) {
                session.setAttribute("error", "AP 처리 실패: " + plain.path("message").asText(""));
                return "redirect:/survey-result";
            }

            SurveySubmitRes result = objectMapper.convertValue(plain.path("data"), SurveySubmitRes.class);
            session.setAttribute("result", result);
            session.removeAttribute("error");
            return "redirect:/survey-result";

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
