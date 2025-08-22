package com.example.memo.company.dto;

import java.time.LocalDateTime;

import com.example.memo.jpa.entity.company.DcMemberStatus;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DcAccountDetailsDto {
    // 계좌 정보
    private String accountNo;
    private String status;
    private Long balance;
    private LocalDateTime createdAt;
    
    // 가입자 정보
    private String memberName;
    private String birthDate;
    private String memberStatus;
    private String startDate;
    private Long annualSalary;
}