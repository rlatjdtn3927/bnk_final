package com.example.memo.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyLoginResponseDto {
    private Long managerId;     // 기업 담당자 ID
    private String managerName; // 담당자 이름
    private String loginId;     // 로그인 아이디
    private Long companyId;     // 회사 ID
    private String companyName; // 회사명
}