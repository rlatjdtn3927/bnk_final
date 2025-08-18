package com.example.memo.rule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor   // 추가
@AllArgsConstructor
@Builder
public class SurveySubmitRes {
    private String type;   // "안정형" 등
    private Integer score; // 총점
}
