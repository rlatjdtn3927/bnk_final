package com.example.memo.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyLoginResponseDto {
    private Long managerId;
    private String managerName;
    private String loginId;
    private Long companyId;
    private String companyName;
}
