package com.example.memo.rule.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AnswerDTO {
    private String id;           // "q1" ~ "q13"
    private String value;        // 라디오일 때 사용 (문자열 숫자)
    private List<String> values; // 체크박스일 때 사용 (문자열 숫자 배열)
}
