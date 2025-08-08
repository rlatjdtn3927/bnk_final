package com.example.memo.company.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.memo.company.dto.DcMemberRegisterResultDto;
import com.example.memo.company.dto.SubscriberDto;
import com.example.memo.company.dto.SubscriberListDto;
import com.example.memo.jpa.entity.company.DcMember;
import com.example.memo.jpa.entity.company.DcMemberStatus;
import com.example.memo.jpa.repository.company.DcMemberRepository;
import com.example.memo.jpa.repository.company.DcMemberStatusRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    /**
     * 엑셀 업로드된 가입자 데이터의 유효성을 검증합니다.
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
     * 검증이 완료된 가입자 정보를 DB에 최종 등록합니다.
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
     * 특정 회사의 가입자 명부 목록을 조회합니다.
     */
    public List<SubscriberListDto> getSubscribers(JsonNode data) {
        Long companyId = data.get("companyId").asLong();
        String statusFilter = data.get("status").asText("ALL"); // 1. status 값 추출
        log.info("✅ [AP] companyId: {}, status: {}", companyId, statusFilter);

        List<DcMember> members;

        // 2. status 값에 따라 다른 메소드 호출
        if ("ACTIVE".equals(statusFilter)) {
            // DB에 저장된 실제 상태값("재직")으로 조회
            members = dcMemberRepository.findMembersByStatus(companyId, "재직");
        } else if ("INACTIVE".equals(statusFilter)) {
            // DB에 저장된 실제 상태값("퇴직")으로 조회
            members = dcMemberRepository.findMembersByStatus(companyId, "퇴직");
        } else { // "ALL" 또는 그 외의 모든 경우
            members = dcMemberRepository.findMembersWithStatusByCompanyId(companyId);
        }
        
        // 3. 조회된 members를 dtoList로 변환하는 로직은 이전과 동일
        List<SubscriberListDto> dtoList = new ArrayList<>();
        log.info("✅ [AP] DB 조회 결과 건수: {} 건", members.size());
        for (DcMember member : members) {
            SubscriberListDto dto = new SubscriberListDto();

            // 주민번호 복호화 및 생년월일 변환
            try {
                String decryptedRrn = cryptoService.decrypt(member.getRrn());
                dto.setBirthDate(formatBirthdateFromServer(decryptedRrn));
            } catch (Exception e) {
                log.error("주민번호 복호화 실패 (Member ID: {}): {}", member.getId(), e.getMessage());
                dto.setBirthDate("복호화 오류");
            }

            // DcMember 정보 매핑
            dto.setName(member.getName());
            dto.setStartDate(member.getStartDate());
            dto.setAnnualSalary(member.getAnnualSalary());
            dto.setBaseDate(member.getBaseDate());

            // DcMemberStatus 정보 매핑 (null-safe)
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
            
            dtoList.add(dto);
        }

        return dtoList;
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
}
