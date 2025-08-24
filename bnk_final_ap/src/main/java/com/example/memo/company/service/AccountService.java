package com.example.memo.company.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.company.dto.DcAccountDetailsDto;
import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.company.DcMember;
import com.example.memo.jpa.entity.company.DcMemberStatus;
import com.example.memo.jpa.repository.company.DcAccountRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final DcAccountRepository dcAccountRepository;
    private final CryptoService cryptoService; // 생년월일 복호화 위해

    @Transactional(readOnly = true)
    public DcAccountDetailsDto getAccountDetailsByMemberId(Long memberId) {
        DcAccount account = dcAccountRepository.findByDcMember_Id(memberId)
            .orElseThrow(() -> new EntityNotFoundException("해당 가입자의 계좌를 찾을 수 없습니다."));

        DcMember member = account.getDcMember();
        DcMemberStatus memberStatus = member.getDcMemberStatus(); 

        String birthDate = "";
        try {
            birthDate = formatBirthdateFromServer(cryptoService.decrypt(member.getRrn()));
        } catch (Exception e) {
            birthDate = "복호화 오류";
        }

        return DcAccountDetailsDto.builder()
                .accountNo(account.getAccountNo())
                .status(account.getStatus())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .memberName(member.getName())
                .birthDate(birthDate)
                .memberStatus(memberStatus.getStatus()) // 재직 상태
                .startDate(memberStatus.getJoinDate().toString()) // 입사일
                .annualSalary(member.getAnnualSalary()) // 연봉
                .build();
    }
    
    // 주민번호 -> 생년월일 변환 메서드
    private String formatBirthdateFromServer(String rrn) {
        if (rrn == null || rrn.length() < 8) return "";
        
        String birthPart = rrn.substring(0, 6);
        char genderDigit = rrn.charAt(7);
        String yearPrefix;
        

        // 1, 2, 5, 6: 1900년대생
        // 3, 4, 7, 8: 2000년대생
        switch (genderDigit) {
            case '1':
            case '2':
            case '5':
            case '6':
                yearPrefix = "19";
                break;
            case '3':
            case '4':
            case '7':
            case '8':
                yearPrefix = "20";
                break;
            default:
                // 1800년대생 등 예외 케이스는 일단 2000년대로 처리
                yearPrefix = "20"; 
                break;
        }

        String year = yearPrefix + birthPart.substring(0, 2);
        String month = birthPart.substring(2, 4);
        String day = birthPart.substring(4, 6);

        return String.format("%s-%s-%s", year, month, day);
    }
}