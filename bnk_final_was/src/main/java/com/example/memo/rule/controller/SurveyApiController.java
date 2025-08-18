package com.example.memo.rule.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.example.memo.rule.dto.SurveySubmitReq;
import com.example.memo.rule.dto.SurveySubmitRes;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/survey-api")
public class SurveyApiController {



    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody SurveySubmitReq req,
                                    @SessionAttribute(value = "LOGIN_USER_ID", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorBody("UNAUTHORIZED", "로그인이 필요합니다."));
        }
      
        return ResponseEntity.ok(null); // 반드시 {type, score}
    }

    @Getter @AllArgsConstructor
    static class ErrorBody { private String code; private String message; }
}
