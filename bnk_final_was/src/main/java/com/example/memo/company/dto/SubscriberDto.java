package com.example.memo.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberDto {
    private String ssn;        // 주민등록번호 (암호화 전)
    private String name;
    private String entryDate;  // 입사일
    private String baseDate;  // 기산일
    private String annualSalary; // 연간임금총액
    private String employmentType; // 임직원구분
    private String email;
    private String phone;
    private String status;     // 현재상태
    private String joinDate;   // 퇴직연금 가입일
    
    private Long companyId;
}
