package com.example.memo.company.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.company.dto.CompanyLoginRequestDto;
import com.example.memo.company.dto.CompanyLoginResponseDto;
import com.example.memo.company.dto.CompanyRegisterDto;
import com.example.memo.jpa.entity.company.Company;
import com.example.memo.jpa.entity.company.CompanyManager;
import com.example.memo.jpa.repository.company.CompanyManagerRepository;
import com.example.memo.jpa.repository.company.CompanyRepository;

@Service
@Transactional
public class CompanyService {

	@Autowired
	private CompanyRepository companyRepository;
	
	@Autowired
	private CompanyManagerRepository companyManagerRepository;
	
	
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
	
	
	@Transactional(readOnly = true)
    public CompanyLoginResponseDto login(CompanyLoginRequestDto dto) {
        CompanyManager manager = companyManagerRepository
                .findByLoginId(dto.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (!manager.getPassword().equals(dto.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // Lazy 로딩 오류 방지 → company.getName() 호출하면 안 됨 → fetch join 사용
        Company company = manager.getCompany();

        return CompanyLoginResponseDto.builder()
                .managerId(manager.getId())
                .managerName(manager.getName())
                .loginId(manager.getLoginId())
                .companyId(company.getId())
                .companyName(company.getName())
                .build();
    }

	





	
}
