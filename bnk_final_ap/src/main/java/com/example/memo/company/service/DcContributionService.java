package com.example.memo.company.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.company.dto.ContribValidationItemDto;
import com.example.memo.company.dto.ContribValidationResultDto;
import com.example.memo.company.dto.DcMemberLite;
import com.example.memo.jpa.entity.company.Company;
import com.example.memo.jpa.entity.company.CompanyManager;
import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.company.DcContributionBatch;
import com.example.memo.jpa.entity.company.DcContributionItem;
import com.example.memo.jpa.entity.company.DcMember;
import com.example.memo.jpa.repository.company.CompanyManagerRepository;
import com.example.memo.jpa.repository.company.CompanyRepository;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.company.DcContributionBatchRepository;
import com.example.memo.jpa.repository.company.DcMemberRepository;
import com.example.memo.jpa.repository.company.DcMemberStatusRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DcContributionService {

    private final DcContributionBatchRepository batchRepo;
    private final DcMemberRepository dcMemberRepo;
    private final DcMemberStatusRepository statusRepo;
    private final DcAccountRepository accountRepo;
    private final CompanyRepository companyRepo;
    private final CompanyManagerRepository managerRepo;
    private final ObjectMapper objectMapper;
    private final CryptoService cryptoService;

    /** 배치 생성: 업로드 직후 초안(DRAFT) 저장 */
    @Transactional
    public Long createContributionBatch(JsonNode data) throws Exception {
        Long companyId  = data.get("companyId").asLong();
        Long uploaderId = data.get("uploaderId").asLong();

        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));
        CompanyManager uploader = managerRepo.findById(uploaderId)
                .orElseThrow(() -> new EntityNotFoundException("Uploader not found"));

        // 클라이언트는 paymentDate로 보냄 → 내부는 planDate로 사용
        LocalDate planDate = LocalDate.parse(data.get("paymentDate").asText());

        List<JsonNode> itemNodes = objectMapper.convertValue(
                data.get("items"), new TypeReference<List<JsonNode>>() {});

        DcContributionBatch batch = DcContributionBatch.builder()
                .company(company)
                .uploader(uploader)
                .fileName(data.get("fileName").asText(""))
                .status(DcContributionBatch.BatchStatus.DRAFT)
                .uploadedAt(LocalDateTime.now())
                .planDate(planDate)
                .build();

        long totalAmount = 0L;
        for (JsonNode n : itemNodes) {
            String ssn   = text(n, "ssn");
            long amount  = parseLong(text(n, "amount"));
            long bonus   = parseLong(text(n, "performanceBonus"));
            long money   = amount + bonus;
            totalAmount += money;

            DcMember member = null;
            if (!ssn.isBlank()) {
                String enc = cryptoService.encrypt(ssn);
                member = dcMemberRepo.findByRrn(enc).orElse(null);
            }

            DcContributionItem item = DcContributionItem.builder()
                    .dcMember(member)
                    .amount(money)
                    .build();
            batch.addItem(item);
        }

        batch.setTotalRecords(batch.getItems().size());
        batch.setTotalAmount(totalAmount);

        return batchRepo.save(batch).getId();
    }

    /** 검증: 전부 통과일 때만 VALIDATED, 일부라도 실패하면 DRAFT 유지 */
    @Transactional
    public ContribValidationResultDto validateBatch(Long batchId) {
        DcContributionBatch batch = batchRepo.findBatchWithItemsById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        // 미리 멤버 수집
        Set<Long> memberIds = batch.getItems().stream()
                .map(DcContributionItem::getDcMember)
                .filter(Objects::nonNull)
                .map(DcMember::getId)
                .collect(Collectors.toSet());

        // 계좌 활성 맵
        Map<Long, Boolean> accountActive = new HashMap<>();
        for (DcAccount a : accountRepo.findByMemberIds(memberIds)) {
            Long memberId = a.getDcMember() != null ? a.getDcMember().getId() : null;
            if (memberId == null) continue; // 혹시 null이면 스킵

            boolean active = "ACTIVE".equalsIgnoreCase(a.getStatus()); // status 값으로 활성 판단
            accountActive.put(memberId, active);
        }

        // 상태 맵(최신)
        Map<Long, String> statusMap = toMap(statusRepo.findLatestStatusPairs(memberIds));
        // 연봉 맵
        Map<Long, Long> annualMap = toMapLong(dcMemberRepo.findAnnualSalaryPairs(memberIds));
        // (선택) 잔고 맵
        Map<Long, Long> balanceMap = toMapLong(accountRepo.findBalancePairs(memberIds));

        int ok = 0, err = 0;
        List<ContribValidationItemDto> itemDtos = new ArrayList<>();

        for (DcContributionItem it : batch.getItems()) {
            String error = null;
            String vstat = "SUCCESS";
            DcMemberLite lite = DcMemberLite.builder().name("(알 수 없는 가입자)").build();

            DcMember m = it.getDcMember();
            if (m == null) {
                vstat = "FAIL";
                error = "존재하지 않는 가입자입니다.";
            } else {
                lite.setName(nullSafe(m.getName()));

                // 회사 일치 (company 연관관계 X → companyId로 비교)
                Long memberCompanyId = m.getCompanyId();
                Long batchCompanyId  = (batch.getCompany() != null ? batch.getCompany().getId() : null);

                if (!Objects.equals(memberCompanyId, batchCompanyId)) {
                    vstat = "FAIL";
                    error = "해당 회사 소속 회원이 아닙니다.";
                }

                // DC 계좌 보유/활성 (DC-deposit-01)
                if (error == null) {
                    boolean active = accountActive.getOrDefault(m.getId(), false);
                    if (!active) {
                        vstat = "FAIL";
                        error = "DC 계좌가 없거나 비활성입니다.";
                    }
                }

                // 재직 상태 (DC-deposit-02)
                if (error == null) {
                    String st = statusMap.getOrDefault(m.getId(), "UNKNOWN");
                    if (!("재직".equals(st) || "ACTIVE".equalsIgnoreCase(st))) {
                        vstat = "FAIL";
                        error = "납입 대상 상태가 아닙니다(" + st + ").";
                    }
                }

                // 금액 > 0
                if (error == null && (it.getAmount() == null || it.getAmount() <= 0)) {
                    vstat = "FAIL";
                    error = "납입금액은 0보다 커야 합니다.";
                }

                // 연임총액 1/12 이하 (DC-deposit-03)
                if (error == null) {
                    long annual = annualMap.getOrDefault(m.getId(), 0L);
                    long limit = annual > 0 ? (annual / 12L) : Long.MAX_VALUE;
                    if (it.getAmount() != null && it.getAmount() > limit) {
                        vstat = "FAIL";
                        error = "납입금액이 한도(연임총액 1/12) 초과입니다.";
                    }
                }

                // (선택) 잔고 0 (DC-deposit-04) — 정책에 따라 경고/실패 결정
                if (error == null) {
                    long bal = balanceMap.getOrDefault(m.getId(), 0L);
                    if (bal != 0L) {
                        vstat = "FAIL";
                        error = "납입 시점 계좌 잔고가 0원이 아닙니다.";
                    }
                }
            }

            if ("SUCCESS".equals(vstat)) ok++; else err++;

            it.setValidationStatus("SUCCESS".equals(vstat)
                    ? DcContributionItem.ValidationStatus.SUCCESS
                    : DcContributionItem.ValidationStatus.FAIL);
            it.setErrorMessage(error);

            itemDtos.add(ContribValidationItemDto.builder()
                    .itemId(it.getId())
                    .amount(it.getAmount() == null ? 0L : it.getAmount())
                    .validationStatus(vstat)
                    .errorMessage(error)
                    .dcMember(lite)
                    .build());
        }


        batch.setOkCount(ok);
        batch.setErrorCount(err);
        batch.setStatus(err == 0 && ok > 0 ? DcContributionBatch.BatchStatus.VALIDATED
                                           : DcContributionBatch.BatchStatus.DRAFT);

        return ContribValidationResultDto.builder()
                .batchId(batch.getId())
                .fileName(nullSafe(batch.getFileName()))
                .totalRecords(batch.getTotalRecords())
                .okCount(ok)
                .errorCount(err)
                .items(itemDtos)
                .build();
    }
    
    @Transactional
    public Map<String, Object> confirmBatch(Long batchId, Long companyId) {
        DcContributionBatch batch = batchRepo.findBatchWithItemsById(batchId)
            .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        // 소유 회사 확인
        Long batchCompanyId = (batch.getCompany() != null ? batch.getCompany().getId() : null);
        if (!Objects.equals(batchCompanyId, companyId)) {
            throw new IllegalStateException("해당 회사 배치가 아닙니다.");
        }

        // 상태/오류 체크
        if (batch.getStatus() != DcContributionBatch.BatchStatus.VALIDATED) {
            throw new IllegalStateException("검증 완료 상태가 아닙니다.");
        }
        if (batch.getErrorCount() != null && batch.getErrorCount() > 0) {
            throw new IllegalStateException("오류 건이 존재하여 확정할 수 없습니다.");
        }

        // 확정
        batch.setStatus(DcContributionBatch.BatchStatus.CONFIRMED);

        // 응답 요약
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("batchId", batch.getId());
        resp.put("status", batch.getStatus().name());
        resp.put("okCount", Optional.ofNullable(batch.getOkCount()).orElse(0));
        resp.put("totalAmount", Optional.ofNullable(batch.getTotalAmount()).orElse(0L));
        return resp;
    }

    // ===== util =====
    private static String text(JsonNode n, String f) { return (n.hasNonNull(f) ? n.get(f).asText() : ""); }
    private static long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        return Long.parseLong(s.replaceAll("[,\\s]", ""));
    }
    private static Map<Long, String> toMap(List<Object[]> rows) {
        Map<Long, String> m = new HashMap<>();
        for (Object[] r : rows) m.put(((Number) r[0]).longValue(), String.valueOf(r[1]));
        return m;
    }
    private static Map<Long, Long> toMapLong(List<Object[]> rows) {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put(((Number) r[0]).longValue(), r[1]==null?0L:((Number) r[1]).longValue());
        return m;
    }
    private static String nullSafe(String s) { return s == null ? "" : s; }
}