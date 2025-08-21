package com.example.memo.rule.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SurveyViewController {

    @GetMapping("/survey-view")
    public String showSurveyPage(HttpSession session) {
        // ✅ 새로운 설문 시작 전에 기존 결과 세션 초기화
        session.removeAttribute("result");
        session.removeAttribute("error");
        
        return "rule/survey"; // templates/survey.html 렌더링
    }
}