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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.company.dto.ContribPlanListResponse;
import com.example.memo.company.dto.ContribPlanListRowDto;
import com.example.memo.company.dto.ContribValidationItemDto;
import com.example.memo.company.dto.ContribValidationResultDto;
import com.example.memo.company.dto.DcMemberLite;
import com.example.memo.jpa.entity.company.Company;
import com.example.memo.jpa.entity.company.CompanyAccount;
import com.example.memo.jpa.entity.company.CompanyManager;
import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.company.DcContributionBatch;
import com.example.memo.jpa.entity.company.DcContributionItem;
import com.example.memo.jpa.entity.company.DcDepositHistory;
import com.example.memo.jpa.entity.company.DcMember;
import com.example.memo.jpa.repository.company.CompanyAccountRepository;
import com.example.memo.jpa.repository.company.CompanyManagerRepository;
import com.example.memo.jpa.repository.company.CompanyRepository;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.company.DcContributionBatchRepository;
import com.example.memo.jpa.repository.company.DcDepositHistoryRepository;
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
    private final DcDepositHistoryRepository dcDepositHistoryRepository;
    private final DcMemberRepository dcMemberRepo;
    private final DcMemberStatusRepository statusRepo;
    private final DcAccountRepository accountRepo;
    private final CompanyRepository companyRepo;
    private final CompanyManagerRepository managerRepo;
    private final CompanyAccountRepository companyAccountRepository;
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

             // 연임총액 1/12 이상(법정 최소 납입금액) 검사 
                if (error == null) {
                    long annual = annualMap.getOrDefault(m.getId(), 0L);
                    if (annual <= 0) {
                        vstat = "FAIL";
                        error = "연간임금총액(annualSalary)이 설정되지 않았습니다.";
                    } else {
                        // 최소금액: 연임총액 / 12 의 천원단위 올림 등 정책에 맞게 반올림 규칙 결정
                        long min = (annual + 11) / 12;            // = Math.ceil(annual/12.0) 의 정수 버전
                        long amt = Optional.ofNullable(it.getAmount()).orElse(0L);
                        if (amt < min) {
                            vstat = "FAIL";
                            error = "최소 납입금액(연임총액 1/12) 미만입니다. 최소 " + String.format("%,d", min) + "원";
                        }
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
    
    @Transactional(readOnly = true)
    public ContribPlanListResponse listBatches(Long companyId, LocalDate from, LocalDate to,
                                               DcContributionBatch.BatchStatus status,
                                               String keyword, int page, int size) {
        Page<DcContributionBatch> p = batchRepo.findList(
                companyId, from, to, status,
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                PageRequest.of(Math.max(page,0), Math.max(size,1))
        );

        List<ContribPlanListRowDto> rows = p.getContent().stream().map(b ->
            ContribPlanListRowDto.builder()
                .batchId(b.getId())
                .planDate(b.getPlanDate() == null ? null : b.getPlanDate().toString())
                .status(b.getStatus() == null ? null : b.getStatus().name())
                .totalRecords(b.getTotalRecords())
                .okCount(Optional.ofNullable(b.getOkCount()).orElse(0))
                .errorCount(Optional.ofNullable(b.getErrorCount()).orElse(0))
                .totalAmount(Optional.ofNullable(b.getTotalAmount()).orElse(0L))
                .fileName(b.getFileName())
                .uploadedAt(b.getUploadedAt() == null ? null : b.getUploadedAt().toString().replace('T',' '))
                .build()
        ).toList();

        return ContribPlanListResponse.builder()
                .rows(rows)
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .build();
    }
    
    
    @Transactional
    public Map<String,Object> executeBatch(Long batchId, Long companyId, Long sourceAccountId) {
        DcContributionBatch b = batchRepo.findBatchWithItemsById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        if (!Objects.equals(b.getCompany().getId(), companyId))
            throw new IllegalStateException("회사 불일치");

        if (b.getStatus() != DcContributionBatch.BatchStatus.CONFIRMED)
            throw new IllegalStateException("확정 상태가 아닙니다.");

        long total = b.getItems().stream()
                .filter(i -> i.getValidationStatus() == DcContributionItem.ValidationStatus.SUCCESS)
                .mapToLong(i -> Optional.ofNullable(i.getAmount()).orElse(0L))
                .sum();
        if (total <= 0) throw new IllegalStateException("입금할 금액이 없습니다.");

        CompanyAccount src = companyAccountRepository.findByIdAndCompanyId(sourceAccountId, companyId)
                .orElseThrow(() -> new IllegalStateException("출금계좌를 찾을 수 없습니다."));

        long srcBal = parseLongSafe(src.getBalance());
        if (srcBal < total) throw new IllegalStateException("출금계좌 잔액 부족");

        // 출금
        src.setBalance(Long.toString(srcBal - total));

        // 입금 + 이력
        LocalDateTime now = LocalDateTime.now();
        for (DcContributionItem it : b.getItems()) {
            if (it.getValidationStatus() != DcContributionItem.ValidationStatus.SUCCESS) continue;

            DcMember m = it.getDcMember();
            DcAccount dest = accountRepo.findByDcMember_Id(m.getId())
                    .orElseThrow(() -> new IllegalStateException("DC계좌 없음: " + m.getId()));

            long dBal = Optional.ofNullable(dest.getBalance()).orElse(0L);
            dest.setBalance(dBal + it.getAmount());

            DcDepositHistory h = DcDepositHistory.builder()
                    .item(it)
                    .sourceAccount(src)
                    .destinationAccount(dest)
                    .paidAmount(it.getAmount())
                    .paidAt(now)
                    .build();
            dcDepositHistoryRepository.save(h);
        }

        b.setStatus(DcContributionBatch.BatchStatus.EXECUTED);

        return Map.of("batchId", b.getId(),
                      "status", b.getStatus().name(),
                      "paidAmount", total,
                      "paidAt", now.toString());
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

    private long parseLongSafe(String v) {
        if (v == null) return 0L;
        String s = v.replaceAll("[^0-9\\-]", "");
        if (s.isEmpty() || "-".equals(s)) return 0L;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }
}