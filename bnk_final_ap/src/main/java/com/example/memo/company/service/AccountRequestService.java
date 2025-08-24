package com.example.memo.company.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.company.dto.AccountRequestListDto;
import com.example.memo.jpa.entity.admin.BankEmployee;
import com.example.memo.jpa.entity.company.CompanyManager;
import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.company.DcAccountRequest;
import com.example.memo.jpa.entity.company.DcMember;
import com.example.memo.jpa.repository.admin.BankEmployeeRepository;
import com.example.memo.jpa.repository.company.CompanyManagerRepository;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.company.DcAccountRequestRepository;
import com.example.memo.jpa.repository.company.DcMemberRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AccountRequestService {

    private final DcAccountRequestRepository dcAccountRequestRepository;
    private final DcMemberRepository dcMemberRepository;
    private final CompanyManagerRepository companyManagerRepository; // 기업 담당자 조회를 위해 주입
    private final DcAccountRepository dcAccountRepository;
    private final BankEmployeeRepository bankEmployeeRepository;
    
    public AccountRequestService(DcAccountRequestRepository dcAccountRequestRepository,
            DcMemberRepository dcMemberRepository,
            CompanyManagerRepository companyManagerRepository,
            DcAccountRepository dcAccountRepository,
            BankEmployeeRepository bankEmployeeRepository) {
	this.dcAccountRequestRepository = dcAccountRequestRepository;
	this.dcMemberRepository = dcMemberRepository;
	this.companyManagerRepository = companyManagerRepository;
	this.dcAccountRepository = dcAccountRepository;
	this.bankEmployeeRepository = bankEmployeeRepository;
	}
    /**
     * 계좌 개설 요청
     * @param memberIds 신청 대상 가입자 ID 목록
     * @param requestedById 신청을 요청한 기업 담당자의 ID
     * @return 성공적으로 처리된 요청 건수
     */
    @Transactional
    public int createAccountRequests(List<String> memberIds, Long requestedById) {

        CompanyManager requester = companyManagerRepository.findById(requestedById)
                .orElseThrow(() -> new EntityNotFoundException("..."));

        int successCount = 0; // 성공 건수만 세도록 변경

        for (String memberIdStr : memberIds) {
            Long memberId = Long.parseLong(memberIdStr);
            DcMember member = dcMemberRepository.findById(memberId)
                    .orElseThrow(() -> new EntityNotFoundException("..."));

            // 1. 중복 계좌 확인 
            boolean hasAccount = dcAccountRepository.existsByDcMember(member);
            if (hasAccount) {
                System.out.println("이미 계좌가 있는 회원입니다: " + member.getName()); // 로그 출력
                continue; // 다음 회원으로 넘어감
            }

            // 2. 처리중인 요청 확인 
            boolean isPending = dcAccountRequestRepository.existsByDcMemberAndStatus(member, "PENDING");
            if (isPending) {
                System.out.println("이미 처리중인 요청이 있는 회원입니다: " + member.getName());
                continue; // 다음 회원으로 넘어감
            }

            // 모든 확인 통과 시 요청 생성
            DcAccountRequest request = new DcAccountRequest();
            request.setStatus("PENDING");
            request.setDcMember(member);
            request.setRequestedBy(requester);
            
            dcAccountRequestRepository.save(request);
            successCount++; // 성공 건수 증가
        }
        return successCount; // 실제 성공한 건수만 반환
    }	
    
    
    /**
     * 승인 대기 중인 모든 계좌 개설 요청을 조회합니다.
     * @return 승인 대기 상태의 요청 목록
     */
    @Transactional(readOnly = true)
    public Map<String, List<AccountRequestListDto>> getPendingRequestsGroupedByCompany() {
        
        List<AccountRequestListDto> pendingRequests = dcAccountRequestRepository
                .findByStatus("PENDING").stream()
                .map(AccountRequestListDto::fromEntity)
                .collect(Collectors.toList());

        // Java Stream의 groupingBy를 사용하여 회사 이름(companyName)으로 데이터를 그룹핑
        return pendingRequests.stream()
                .collect(Collectors.groupingBy(AccountRequestListDto::getCompanyName));
    }
    
    
    /**
     * 계좌 개설 요청 승인 처리
     * @param requestId 승인할 요청의 ID
     * @param approverId 승인을 처리한 은행 직원의 ID
     */
    @Transactional
    public void approveRequest(Long requestId, Long approverId) {
        // 1. 승인 요청 엔티티와 승인자 엔티티를 조회
        DcAccountRequest request = dcAccountRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));
        
        BankEmployee approver = bankEmployeeRepository.findById(approverId)
                .orElseThrow(() -> new EntityNotFoundException("Bank Employee not found with id: " + approverId));

        // 2. 요청 상태를 'APPROVED'로 변경하고, 처리 정보 기록
        request.setStatus("APPROVED");
        request.setApprovedBy(approver);
        request.setProcessedAt(LocalDateTime.now());

        // 3. 새로운 DC 계좌 생성
        DcAccount newAccount = new DcAccount();
        newAccount.setAccountNo(generateDcAccountNumber()); // 계좌번호 생성
        newAccount.setStatus("ACTIVE");
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setDcMember(request.getDcMember());

        // 4. 생성된 계좌를 저장하고, 요청서에도 연결
        DcAccount savedAccount = dcAccountRepository.save(newAccount);
        request.setDcAccount(savedAccount);

        dcAccountRequestRepository.save(request);
    }
    
    
    /**
     * "DC-날짜-일련번호" 형식의 계좌번호를 생성
     * @return 생성된 계좌번호 (예: DC-20250811-0001)
     */
    private String generateDcAccountNumber() {
        String prefix = "DC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long countToday = dcAccountRepository.countByAccountNoStartingWith(prefix);
        String sequence = String.format("%04d", countToday + 1);
        return prefix + "-" + sequence;
    }
    
    /**
     * 계좌 개설 요청 거절 처리
     * @param requestId 거절할 요청의 ID
     * @param approverId 처리를 한 은행 직원의 ID
     * @param reason 거절 사유
     */
    @Transactional
    public void rejectRequest(Long requestId, Long approverId, String reason) {
        // 1. 요청 엔티티와 처리자 엔티티를 조회
        DcAccountRequest request = dcAccountRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + requestId));
        
        BankEmployee approver = bankEmployeeRepository.findById(approverId)
                .orElseThrow(() -> new EntityNotFoundException("Bank Employee not found with id: " + approverId));

        // 2. 요청 상태를 'REJECTED'로 변경하고, 처리 정보를 기록
        request.setStatus("REJECTED");
        request.setRejectionReason(reason); // 거절 사유 저장
        request.setApprovedBy(approver);    // 처리한 직원 정보 설정
        request.setProcessedAt(LocalDateTime.now());

        // 3. 변경된 요청 상태를 저장
        dcAccountRequestRepository.save(request);
    }
    
    /**
     * 여러 계좌 개설 요청을 한번에 승인 처리
     * @param requestIds 승인할 요청 ID 목록
     * @param approverId 승인자 ID
     */
    @Transactional
    public void approveRequests(List<Long> requestIds, Long approverId) {
        requestIds.forEach(requestId -> approveRequest(requestId, approverId));
    }

    /**
     * 여러 계좌 개설 요청을 한번에 거절 처리
     * @param requestIds 거절할 요청 ID 목록
     * @param approverId 처리자 ID
     * @param reason 공통 거절 사유
     */
    @Transactional
    public void rejectRequests(List<Long> requestIds, Long approverId, String reason) {
        requestIds.forEach(requestId -> rejectRequest(requestId, approverId, reason));
    }
    
    /**
     * 상태별 요청 목록을 회사명으로 그룹핑해서 반환 (정렬: 신청일시 DESC)
     * 스키마: Map<회사명, List<AccountRequestListDto>>
     */
    @Transactional(readOnly = true)
    public Map<String, List<AccountRequestListDto>> getRequestsGroupedByCompany(String status) {
        // 성능/일관성을 위해 정렬 포함 메서드 권장
        var rows = dcAccountRequestRepository.findByStatusOrderByRequestedAtDesc(status);

        var list = rows.stream()
                .map(AccountRequestListDto::fromEntity)
                .collect(Collectors.toList());

        // 같은 회사끼리 묶되, 표시 순서를 요청일자 기준으로 자연스럽게 유지하고 싶으면 LinkedHashMap 사용
        return list.stream().collect(Collectors.groupingBy(
                AccountRequestListDto::getCompanyName
                // 기본 groupingBy는 HashMap이지만, 표시 순서 중요하면 아래 주석 해제
                 , LinkedHashMap::new, Collectors.toList()
        ));
    }
    
    
    
}