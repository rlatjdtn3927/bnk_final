package com.example.memo.company.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.memo.company.dto.CompanyRegisterDto;
import com.example.memo.jpa.entity.company.Company;
import com.example.memo.jpa.repository.company.CompanyRepository;

@Service
public class CompanyService {

	@Autowired
	private CompanyRepository companyRepository;
	
	public String registerCompany(CompanyRegisterDto dto) {
        // 1. 사업자등록번호 중복 체크
        if (companyRepository.existsByBizNo(dto.getBizNo())) {
            return "이미 등록된 사업자등록번호입니다.";
        }

        // 2. DTO → Entity 변환
        Company company = Company.builder()
            .bizNo(dto.getBizNo())
            .name(dto.getName())
            .ceoName(dto.getCeoName())
            .phone(dto.getPhone())
            .email(dto.getEmail())
            .address(dto.getAddress())
            .build();

        // 3. 저장
        companyRepository.save(company);
        return "기업 등록 성공";
    }
	
}
