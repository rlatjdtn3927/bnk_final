package com.example.memo.rule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SurveySubmitRes {
    private String type;   // "안정형" 등
    private Integer score; // 총점
}
