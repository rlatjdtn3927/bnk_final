package com.example.memo.rule.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SurveyViewController {

    @GetMapping("/survey-view")
    public String showSurveyPage() {
        return "rule/survey"; // templates/survey.html 렌더링
    }
}
