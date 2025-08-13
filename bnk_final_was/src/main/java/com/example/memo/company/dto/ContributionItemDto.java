package com.example.memo.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContributionItemDto {
    private String name;              // 성명
    private String ssn;               // 주민등록번호
    private String amount;            // 기업부담금
    private String performanceBonus;  // 경영성과급(선택)
    private String annualSalary;      // 연간임금총액(선택)
}
