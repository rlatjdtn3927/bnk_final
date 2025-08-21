package com.example.memo.rule.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SurveySubmitReq {
    private String surveyCode;         // 선택: "RETIRE-2025-01" 등
    private List<AnswerDTO> answers;   // 필수: 문항 배열

    // ✅ 기본값 보장
    private Map<String, Object> meta = new HashMap<>();
}
