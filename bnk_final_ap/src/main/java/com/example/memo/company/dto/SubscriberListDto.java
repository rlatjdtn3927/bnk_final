package com.example.memo.company.dto;

import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubscriberListDto {
	
	private Long memberId;
	private String accountStatus;
	
    // DcMember 정보
    private String name;
    private String birthDate;
    private LocalDate startDate;
    private Long annualSalary;
    private LocalDate baseDate;

    // DcMemberStatus 정보
    private String status;
    private LocalDate joinDate;
    private LocalDate retireDate;
    private LocalDate cancelDate;
    private LocalDate firstPayDate;
    private Double dbRatio; // Integer -> Double
    private Double dcRatio; // Integer -> Double

}