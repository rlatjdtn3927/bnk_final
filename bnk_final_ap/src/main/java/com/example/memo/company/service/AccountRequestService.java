package com.example.memo.company.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.company.CompanyManager;
import com.example.memo.jpa.entity.company.DcAccountRequest;
import com.example.memo.jpa.entity.company.DcMember;
import com.example.memo.jpa.repository.company.CompanyManagerRepository;
import com.example.memo.jpa.repository.company.DcAccountRequestRepository;
import com.example.memo.jpa.repository.company.DcMemberRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AccountRequestService {

    private final DcAccountRequestRepository dcAccountRequestRepository;
    private final DcMemberRepository dcMemberRepository;
    private final CompanyManagerRepository companyManagerRepository; // 기업 담당자 조회를 위해 주입

    public AccountRequestService(DcAccountRequestRepository dcAccountRequestRepository,
                                 DcMemberRepository dcMemberRepository,
                                 CompanyManagerRepository companyManagerRepository) {
        this.dcAccountRequestRepository = dcAccountRequestRepository;
        this.dcMemberRepository = dcMemberRepository;
        this.companyManagerRepository = companyManagerRepository;
    }

    /**
     * 계좌 개설 요청을 생성합니다.
     * @param memberIds 신청 대상 가입자 ID 목록
     * @param requestedById 신청을 요청한 기업 담당자의 ID
     * @return 성공적으로 처리된 요청 건수
     */
    @Transactional
    public int createAccountRequests(List<String> memberIds, Long requestedById) {

        // 1. 요청을 보낸 기업 담당자(CompanyManager) 엔티티를 먼저 조회합니다.
        //    이 담당자가 존재하지 않으면, 이후 로직은 의미가 없으므로 예외를 발생시킵니다.
        CompanyManager requester = companyManagerRepository.findById(requestedById)
                .orElseThrow(() -> new EntityNotFoundException("Company Manager not found with id: " + requestedById));

        for (String memberIdStr : memberIds) {
            Long memberId = Long.parseLong(memberIdStr);

            // 2. 각 가입자(DcMember) 엔티티를 조회합니다.
            DcMember member = dcMemberRepository.findById(memberId)
                    .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + memberId));

            // 3. DcAccountRequest 엔티티를 생성하고, 필요한 관계를 설정합니다.
            DcAccountRequest request = new DcAccountRequest();
            request.setStatus("PENDING"); // 초기 상태는 '대기'
            request.setDcMember(member);      // 신청 대상 가입자 설정
            request.setRequestedBy(requester); // 신청한 기업 담당자 설정
            
            // 4. 최종적으로 데이터베이스에 저장합니다.
            dcAccountRequestRepository.save(request);
        }

        return memberIds.size();
    }
}