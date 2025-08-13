package com.example.memo.company.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.memo.company.dto.DcMemberRegisterResultDto;
import com.example.memo.company.dto.SubscriberDto;
import com.example.memo.company.dto.SubscriberListDto;
import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.company.DcAccountRequest;
import com.example.memo.jpa.entity.company.DcMember;
import com.example.memo.jpa.entity.company.DcMemberStatus;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.company.DcAccountRequestRepository;
import com.example.memo.jpa.repository.company.DcMemberRepository;
import com.example.memo.jpa.repository.company.DcMemberStatusRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

//AP 서버
@Service
@RequiredArgsConstructor
public class SubscriberService {

    private static final Logger log = LoggerFactory.getLogger(SubscriberService.class);

    private final DcMemberRepository dcMemberRepository;
    private final DcMemberStatusRepository dcMemberStatusRepository;
    private final CryptoService cryptoService; // 암호화 서비스
    private final ObjectMapper objectMapper;
    private final DcAccountRepository dcAccountRepository;
    private final DcAccountRequestRepository dcAccountRequestRepository;
    private final EntityManager em;
    
    
    /**
     * 엑셀 업로드된 가입자 데이터 유효성 검증
     */
    public List<Map<String, Object>> validateSubscribers(JsonNode data) throws Exception {
        List<SubscriberDto> dtoList = objectMapper.readerForListOf(SubscriberDto.class).readValue(data);
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> ssnInFile = new HashSet<>();

        for (int i = 0; i < dtoList.size(); i++) {
            SubscriberDto dto = dtoList.get(i);
            Map<String, Object> result = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();

            if (dto.getName() == null || dto.getName().isBlank()) errors.add("이름 누락");
            if (dto.getSsn() == null || dto.getSsn().isBlank()) errors.add("주민등록번호 누락");

            if (errors.isEmpty()) {
                if (ssnInFile.contains(dto.getSsn())) {
                    errors.add("파일 내 주민등록번호 중복");
                } else if (dcMemberRepository.existsByRrn(cryptoService.encrypt(dto.getSsn()))) {
                    errors.add("DB에 이미 등록된 주민등록번호");
                }
            }
            
            ssnInFile.add(dto.getSsn());

            result.put("subscriber", dto);
            result.put("valid", errors.isEmpty());
            result.put("errors", errors);
            result.put("row", i + 2);
            results.add(result);
        }
        return results;
    }

    /**
     * 검증이 완료된 가입자 정보를 DB에 최종 등록
     */
    @Transactional
    public DcMemberRegisterResultDto dcMemberRegister(JsonNode data) throws Exception {
        List<SubscriberDto> dtoList = objectMapper.readerForListOf(SubscriberDto.class).readValue(data);
        DcMemberRegisterResultDto resultDto = new DcMemberRegisterResultDto();
        int successCount = 0;

        for (int i = 0; i < dtoList.size(); i++) {
            SubscriberDto dto = dtoList.get(i);
            try {
                DcMember member = DcMember.builder()
                        .rrn(cryptoService.encrypt(dto.getSsn()))
                        .name(dto.getName())
                        .startDate(LocalDate.parse(dto.getEntryDate()))
                        .baseDate(LocalDate.parse(dto.getBaseDate()))
                        .annualSalary(Long.parseLong(dto.getAnnualSalary()))
                        .employmentType(dto.getEmploymentType())
                        .email(dto.getEmail())
                        .phone(dto.getPhone())
                        .companyId(dto.getCompanyId())
                        .build();

                DcMemberStatus status = DcMemberStatus.builder()
                        .status(dto.getStatus())
                        .joinDate(LocalDate.parse(dto.getJoinDate()))
                        .build();

                member.setDcMemberStatus(status);

                dcMemberRepository.save(member);
                dcMemberStatusRepository.save(status);

                successCount++;
            } catch (Exception e) {
                log.error("가입자 등록 실패 (행: {}): {}", i + 2, e.getMessage());
                resultDto.addError(i + 2, "등록 중 서버 오류 발생");
            }
        }
        resultDto.setResult(dtoList.size(), successCount);
        return resultDto;
    }

    /**
     * 조건에 맞는 회사의 가입자 명부 목록과 '계좌 상태'를 함께 조회
     * @param data 필터 조건 (companyId, name, status)
     * @return 가입자 정보 DTO 목록
     */
    public List<SubscriberListDto> getSubscribers(JsonNode data) {
        Long companyId = data.get("companyId").asLong();
        String name = data.get("name").asText(null);
        String employmentStatus = data.get("status").asText("ALL");
        String accountStatus = data.get("accountStatus").asText("ALL");

        // 1. 서비스 내의 private 메서드를 호출하여 조건에 맞는 가입자 목록 조회
        List<DcMember> members = findMembersByCriteria(companyId, name, employmentStatus, accountStatus);

        if (members.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. N+1 방지를 위한 정보 미리 조회
        List<Long> memberIds = members.stream().map(DcMember::getId).collect(Collectors.toList());
        Map<Long, DcAccount> accountMap = dcAccountRepository.findByDcMemberIdIn(memberIds).stream()
                .collect(Collectors.toMap(acc -> acc.getDcMember().getId(), acc -> acc));
        Map<Long, DcAccountRequest> pendingRequestMap = dcAccountRequestRepository.findByDcMemberIdInAndStatus(memberIds, "PENDING").stream()
                .collect(Collectors.toMap(req -> req.getDcMember().getId(), req -> req, (r1, r2) -> r1));

        // 3. DTO 조립
        return members.stream().map(member -> {
            SubscriberListDto dto = new SubscriberListDto();
            dto.setMemberId(member.getId());

            if (accountMap.containsKey(member.getId())) {
                dto.setAccountStatus("개설 완료");
            } else if (pendingRequestMap.containsKey(member.getId())) {
                dto.setAccountStatus("처리중");
            } else {
                dto.setAccountStatus("신청 가능");
            }

            try {
                String decryptedRrn = cryptoService.decrypt(member.getRrn());
                dto.setBirthDate(formatBirthdateFromServer(decryptedRrn));
            } catch (Exception e) {
                log.error("주민번호 복호화 실패 (Member ID: {}): {}", member.getId(), e.getMessage());
                dto.setBirthDate("복호화 오류");
            }
            dto.setName(member.getName());
            dto.setStartDate(member.getStartDate());
            dto.setAnnualSalary(member.getAnnualSalary());
            dto.setBaseDate(member.getBaseDate());
            if (member.getDcMemberStatus() != null) {
                DcMemberStatus status = member.getDcMemberStatus();
                dto.setStatus(status.getStatus());
                dto.setJoinDate(status.getJoinDate());
                dto.setRetireDate(status.getRetireDate());
                dto.setCancelDate(status.getCancelDate());
                dto.setFirstPayDate(status.getFirstPayDate());
                dto.setDbRatio(status.getDbRatio());
                dto.setDcRatio(status.getDcRatio());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 주민번호 문자열을 생년월일(YYYY-MM-DD) 형식으로 변환하는 내부 헬퍼 메소드
     */
    private String formatBirthdateFromServer(String rrn) {
        if (rrn == null || rrn.length() < 8) return "";
        
        String birthPart = rrn.substring(0, 6);
        char genderDigit = rrn.charAt(7);
        String yearPrefix;
        
        log.info("### 생년월일 변환 로직 실행! 주민번호 7번째 자리: {}", genderDigit);


        // 1, 2, 5, 6: 1900년대생 (5, 6은 외국인)
        // 3, 4, 7, 8: 2000년대생 (7, 8은 외국인)
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
    
    private List<DcMember> findMembersByCriteria(Long companyId, String name, String employmentStatus, String accountStatus) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<DcMember> cq = cb.createQuery(DcMember.class);
        Root<DcMember> member = cq.from(DcMember.class);
        Join<DcMember, DcMemberStatus> statusJoin = member.join("dcMemberStatus");
        
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(member.get("companyId"), companyId));

        if (name != null && !name.isBlank()) {
            predicates.add(cb.like(member.get("name"), "%" + name + "%"));
        }
        if ("ACTIVE".equals(employmentStatus)) {
            predicates.add(cb.equal(statusJoin.get("status"), "재직"));
        } else if ("INACTIVE".equals(employmentStatus)) {
            predicates.add(cb.equal(statusJoin.get("status"), "퇴직"));
        }
        if (accountStatus != null && !"ALL".equals(accountStatus)) {
            if ("COMPLETED".equals(accountStatus)) {
                predicates.add(cb.exists(subqueryForAccount(cb, cq, member)));
            } else if ("PENDING".equals(accountStatus)) {
                predicates.add(cb.exists(subqueryForRequest(cb, cq, member, "PENDING")));
                predicates.add(cb.not(cb.exists(subqueryForAccount(cb, cq, member))));
            } else if ("POSSIBLE".equals(accountStatus)) {
                predicates.add(cb.not(cb.exists(subqueryForAccount(cb, cq, member))));
                predicates.add(cb.not(cb.exists(subqueryForRequest(cb, cq, member, "PENDING"))));
            }
        }
        
        cq.where(predicates.toArray(new Predicate[0]));
        return em.createQuery(cq).getResultList();
    }

    private Subquery<Integer> subqueryForAccount(CriteriaBuilder cb, CriteriaQuery<?> mainQuery, Root<DcMember> member) {
        Subquery<Integer> subquery = mainQuery.subquery(Integer.class);
        Root<DcAccount> account = subquery.from(DcAccount.class);
        subquery.select(cb.literal(1)).where(cb.equal(account.get("dcMember"), member));
        return subquery;
    }

    private Subquery<Integer> subqueryForRequest(CriteriaBuilder cb, CriteriaQuery<?> mainQuery, Root<DcMember> member, String status) {
        Subquery<Integer> subquery = mainQuery.subquery(Integer.class);
        Root<DcAccountRequest> request = subquery.from(DcAccountRequest.class);
        subquery.select(cb.literal(1)).where(
            cb.and(
                cb.equal(request.get("dcMember"), member),
                cb.equal(request.get("status"), status)
            )
        );
        return subquery;
    }
}
