package com.example.memo.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompanyRegisterDto {

    private String bizNo;     // 사업자등록번호 (예: 000-00-00000)
    private String name;      // 기업명
    private String ceoName;   // 대표자명
    private String phone;     // 연락처
    private String email;     // 이메일
    private String address;   // 주소 
}
