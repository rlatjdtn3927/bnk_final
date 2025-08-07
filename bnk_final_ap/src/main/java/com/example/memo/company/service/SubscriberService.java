package com.example.memo.company.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.memo.company.dto.DcMemberRegisterResultDto;
import com.example.memo.company.dto.SubscriberDto;
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

 private final DcMemberRepository dcMemberRepository;
 private final DcMemberStatusRepository dcMemberStatusRepository;
 private final CryptoService cryptoService; // 암호화 서비스 주입
 private final ObjectMapper objectMapper;   // Bean으로 등록하여 사용 권장

 /**
  * 가입자 데이터 검증 로직
  */
 public List<Map<String, Object>> validateSubscribers(JsonNode data) throws Exception {
     List<SubscriberDto> dtoList = objectMapper.readerForListOf(SubscriberDto.class).readValue(data);
     List<Map<String, Object>> results = new ArrayList<>();
     
     // 파일 내 중복 체크용 Set
     Set<String> ssnInFile = new HashSet<>();

     for (int i = 0; i < dtoList.size(); i++) {
         SubscriberDto dto = dtoList.get(i);
         Map<String, Object> result = new LinkedHashMap<>();
         List<String> errors = new ArrayList<>();

         // 1. 필수 값 검증
         if (dto.getName() == null || dto.getName().isBlank()) errors.add("이름 누락");
         if (dto.getSsn() == null || dto.getSsn().isBlank()) errors.add("주민등록번호 누락");

         // 2. 포맷 및 DB 중복 검증
         if (!errors.isEmpty()) {
             // 필수값 없으면 이후 검증 무의미
         } else if (ssnInFile.contains(dto.getSsn())) {
             errors.add("파일 내 주민등록번호 중복");
         } else if (dcMemberRepository.existsByRrn(cryptoService.encrypt(dto.getSsn()))) {
             errors.add("DB에 이미 등록된 주민등록번호");
         }
         
         ssnInFile.add(dto.getSsn());

         result.put("subscriber", dto);
         result.put("valid", errors.isEmpty());
         result.put("errors", errors);
         result.put("row", i + 2); // 엑셀 행 번호 (헤더 제외)
         results.add(result);
     }
     return results;
 }

 /**
  * 검증 완료된 가입자 등록 로직
  */
 @Transactional
 public DcMemberRegisterResultDto dcMemberRegister(JsonNode data) throws Exception {
     List<SubscriberDto> dtoList = objectMapper.readerForListOf(SubscriberDto.class).readValue(data);
     DcMemberRegisterResultDto resultDto = new DcMemberRegisterResultDto();
     int successCount = 0;

     for (int i = 0; i < dtoList.size(); i++) {
         SubscriberDto dto = dtoList.get(i);
         try {
             // DcMember 생성 및 저장 (암호화 적용)
             DcMember member = DcMember.builder()
                     .rrn(cryptoService.encrypt(dto.getSsn())) // ✅ 중요: 암호화
                     .name(dto.getName())
                     .startDate(LocalDate.parse(dto.getEntryDate()))
                     .baseDate(LocalDate.parse(dto.getBaseDate()))
                     .annualSalary(Long.parseLong(dto.getAnnualSalary()))
                     .employmentType(dto.getEmploymentType())
                     .email(dto.getEmail())
                     .phone(dto.getPhone())
                     .companyId(dto.getCompanyId())
                     .build();
             DcMember saved = dcMemberRepository.save(member);

             // DcMemberStatus 생성 및 저장
             DcMemberStatus status = DcMemberStatus.builder()
                     .status(dto.getStatus())
                     .joinDate(LocalDate.parse(dto.getJoinDate()))
                     .dcMemberId(saved.getId())
                     .build();
             dcMemberStatusRepository.save(status);
             successCount++;

         } catch (Exception e) {
             resultDto.addError(i + 2, e.getMessage()); // 엑셀 행 번호 기준
         }
     }
     resultDto.setResult(dtoList.size(), successCount);
     return resultDto;
 }
}
