package com.example.memo.company.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.company.dto.ContribPlanListResponse;
import com.example.memo.company.dto.ContribPlanListRowDto;
import com.example.memo.company.dto.ContribValidationItemDto;
import com.example.memo.company.dto.ContribValidationResultDto;
import com.example.memo.company.dto.DcMemberLite;
import com.example.memo.company.dto.PayableItemDto;
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
import com.example.memo.jpa.repository.company.DcContributionItemRepository;
import com.example.memo.jpa.repository.company.DcDepositHistoryRepository;
import com.example.memo.jpa.repository.company.DcMemberRepository;
import com.example.memo.jpa.repository.company.DcMemberStatusRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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
    private final DcContributionItemRepository itemRepo;
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

        Set<Long> memberIds = batch.getItems().stream()
                .map(DcContributionItem::getDcMember)
                .filter(Objects::nonNull)
                .map(DcMember::getId)
                .collect(Collectors.toSet());

        // [수정] 필요한 데이터만 미리 조회 (잔고 조회 제거)
        Map<Long, Boolean> accountActiveMap = new HashMap<>();
        accountRepo.findByMemberIds(memberIds).forEach(a -> {
            if (a.getDcMember() != null) {
                boolean isActive = "ACTIVE".equalsIgnoreCase(a.getStatus());
                accountActiveMap.put(a.getDcMember().getId(), isActive);
            }
        });
        Map<Long, String> statusMap = toMap(statusRepo.findLatestStatusPairs(memberIds));
        Map<Long, Long> annualSalaryMap = toMapLong(dcMemberRepo.findAnnualSalaryPairs(memberIds));

        int ok = 0, err = 0;
        List<ContribValidationItemDto> itemDtos = new ArrayList<>();

        for (DcContributionItem it : batch.getItems()) {
            String errorMessage = null;
            DcMember m = it.getDcMember();
            DcMemberLite lite = DcMemberLite.builder().name("(알 수 없는 가입자)").build();

            if (m == null) {
                errorMessage = "존재하지 않는 가입자입니다.";
            } else {
                lite.setName(nullSafe(m.getName()));

                if (!Objects.equals(m.getCompanyId(), batch.getCompany().getId())) {
                    errorMessage = "해당 회사 소속 회원이 아닙니다.";
                }

                // (DC-deposit-01) DC 계좌 보유/활성 검증
                if (errorMessage == null && !accountActiveMap.getOrDefault(m.getId(), false)) {
                    errorMessage = "DC 계좌가 없거나 비활성 상태입니다.";
                }

                // (DC-deposit-02) 재직 상태 검증
                if (errorMessage == null) {
                    String status = statusMap.getOrDefault(m.getId(), "UNKNOWN");
                    if (!("재직".equals(status) || "ACTIVE".equalsIgnoreCase(status))) {
                        errorMessage = "납입 대상 상태가 아닙니다(현재상태: " + status + ").";
                    }
                }

                // [수정] 납입금액 "최소금액" 검증 (연간임금총액 1/12 이상)
                if (errorMessage == null) {
                    long annualSalary = annualSalaryMap.getOrDefault(m.getId(), 0L);
                    if (annualSalary <= 0) {
                        errorMessage = "연간임금총액(annualSalary)이 설정되지 않았습니다.";
                    } else {
                        // 최소금액: 연봉/12 (정수 나눗셈의 올림 효과를 위해 +11)
                        long minimumAmount = (annualSalary + 11) / 12;
                        long amount = Optional.ofNullable(it.getAmount()).orElse(0L);

                        if (amount < minimumAmount) { // 납입금액이 최소금액보다 적은지 검사
                            errorMessage = "최소 납입금액 미만입니다. (최소: " + String.format("%,d", minimumAmount) + "원)";
                        }
                    }
                }
                
                // 금액 > 0 검증
                if (errorMessage == null && (it.getAmount() == null || it.getAmount() <= 0)) {
                    errorMessage = "납입금액은 0보다 커야 합니다.";
                }
            }


            // 검증 결과에 따라 상태 설정
            boolean isSuccess = (errorMessage == null);
            if (isSuccess) {
                ok++;
                it.setValidationStatus(DcContributionItem.ValidationStatus.SUCCESS);
            } else {
                err++;
                it.setValidationStatus(DcContributionItem.ValidationStatus.FAIL);
            }
            it.setErrorMessage(errorMessage);

            itemDtos.add(ContribValidationItemDto.builder()
                    // ... (이하 동일)
                    .itemId(it.getId())
                    .amount(it.getAmount() == null ? 0L : it.getAmount())
                    .validationStatus(isSuccess ? "SUCCESS" : "FAIL")
                    .errorMessage(errorMessage)
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
    
    
    /**
     * 최종 납입 실행: 실행 시점에 재검증 로직을 추가하여 데이터 정합성을 보장합니다.
     * 1. 확정된 배치(CONFIRMED)인지 확인합니다.
     * 2. 검증 시 '성공'이었던 가입자 목록을 가져옵니다.
     * 3. [중요] 실행하는 바로 이 순간, 가입자들의 최신 '재직' 상태를 다시 조회합니다.
     * 4. 최신 상태까지 유효한 가입자만 최종 납입 대상으로 필터링합니다.
     * 5. 최종 대상의 총액을 계산하여 회사 계좌에서 출금합니다.
     * 6. 최종 대상 가입자들의 DC 계좌에 입금하고 내역을 기록합니다.
     * 7. 배치 상태를 '실행완료(EXECUTED)'로 변경합니다.
     *
     * @param batchId         실행할 배치 ID
     * @param companyId       회사의 ID (소유권 확인용)
     * @param sourceAccountId 출금할 회사의 계좌 ID
     * @return 실행 결과 (상태, 처리 금액 등)
     */
    @Transactional
    public Map<String, Object> executeBatch(Long batchId, Long companyId, Long sourceAccountId) {
        // --- 1. 기본 정보 조회 및 검증 ---
        DcContributionBatch b = batchRepo.findBatchWithItemsById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

        if (!Objects.equals(b.getCompany().getId(), companyId)) {
            throw new IllegalStateException("해당 회사의 납입 건이 아닙니다.");
        }

        if (b.getStatus() != DcContributionBatch.BatchStatus.CONFIRMED) {
            throw new IllegalStateException("확정(CONFIRMED) 상태가 아니므로 납입을 실행할 수 없습니다.");
        }

        // --- 2. 검증 시점 '성공' 건 필터링 ---
        List<DcContributionItem> successItems = b.getItems().stream()
                .filter(i -> i.getValidationStatus() == DcContributionItem.ValidationStatus.SUCCESS)
                .toList();

        if (successItems.isEmpty()) {
            throw new IllegalStateException("납입할 대상(성공 건)이 없습니다.");
        }

        // --- 3. [핵심] 실행 시점 재검증 ---
        Set<Long> memberIds = successItems.stream().map(it -> it.getDcMember().getId()).collect(Collectors.toSet());
        Map<Long, String> latestStatusMap = toMap(statusRepo.findLatestStatusPairs(memberIds)); // 최신 상태 DB 조회

        long finalTotalAmount = 0;
        List<DcContributionItem> itemsToPay = new ArrayList<>();

        for (DcContributionItem it : successItems) {
            DcMember m = it.getDcMember();
            String currentStatus = latestStatusMap.getOrDefault(m.getId(), "UNKNOWN");

            // 실행 직전 최종 재검증: 지금도 여전히 재직 상태인지 확인
            if ("재직".equals(currentStatus) || "ACTIVE".equalsIgnoreCase(currentStatus)) {
                itemsToPay.add(it); // 최종 납입 대상에 추가
                finalTotalAmount += Optional.ofNullable(it.getAmount()).orElse(0L);
            } else {
                // 검증 때와 상태가 달라진 경우: 로그를 남기고 납입에서 제외
                // log.warn("납입 실행 제외: 회원 ID {}의 상태가 변경됨 (검증시: 재직, 실행시: {})", m.getId(), currentStatus);
                it.setErrorMessage("실행 시점 상태 변경으로 납입 제외됨 (현재: " + currentStatus + ")");
                // 필요시 실행 상태를 관리하는 별도 컬럼(e.g., executionStatus)을 FAIL로 업데이트 할 수 있습니다.
            }
        }

        if (itemsToPay.isEmpty()) {
            throw new IllegalStateException("최종 납입 대상이 없습니다 (상태 변경 등으로 모두 제외됨).");
        }

        // --- 4. 회사 계좌 출금 (최종 계산된 금액 기준) ---
        // 동시성 문제를 방지하기 위해 PESSIMISTIC_WRITE Lock 사용을 권장합니다.
        // CompanyAccount src = companyAccountRepository.findByIdAndCompanyIdWithLock(sourceAccountId, companyId)
        CompanyAccount src = companyAccountRepository.findByIdAndCompanyId(sourceAccountId, companyId)
                .orElseThrow(() -> new IllegalStateException("출금계좌를 찾을 수 없습니다. (ID: " + sourceAccountId + ")"));

        long srcBalance = parseLongSafe(src.getBalance());
        if (srcBalance < finalTotalAmount) {
            throw new IllegalStateException("출금계좌 잔액이 부족합니다. (필요: " + finalTotalAmount + ", 현재: " + srcBalance + ")");
        }
        src.setBalance(Long.toString(srcBalance - finalTotalAmount));


        // --- 5. 가입자 DC 계좌 입금 및 이력 기록 (최종 필터링된 리스트 기준) ---
        LocalDateTime now = LocalDateTime.now();
        for (DcContributionItem it : itemsToPay) {
            DcMember m = it.getDcMember();
            DcAccount dest = accountRepo.findByDcMember_Id(m.getId())
                    .orElseThrow(() -> new IllegalStateException("가입자의 DC계좌를 찾을 수 없습니다: " + m.getName()));

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

        // --- 6. 배치 상태 최종 변경 ---
        b.setStatus(DcContributionBatch.BatchStatus.EXECUTED);

        return Map.of("batchId", b.getId(),
                      "status", b.getStatus().name(),
                      "paidAmount", finalTotalAmount, // 최종 처리된 금액으로 응답
                      "paidAt", now.toString());
    }
    
    /**
     * 개별 납입 예정 항목 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PayableItemDto> listPayableItems(Long companyId, List<DcContributionBatch.BatchStatus> statuses, LocalDate from, LocalDate to, Pageable pageable) {
        List<DcContributionBatch.BatchStatus> effectiveStatuses = statuses;
        // 만약 상태 목록이 비어있거나 null이면, 조회 가능한 기본 상태 목록을 설정 (예: 확정, 실행)
        if (effectiveStatuses == null || effectiveStatuses.isEmpty()) {
            effectiveStatuses = List.of(DcContributionBatch.BatchStatus.CONFIRMED, DcContributionBatch.BatchStatus.EXECUTED);
        }
        return itemRepo.findPayableItems(companyId, effectiveStatuses, from, to, pageable);
    }
    
    /**
     * [신규] 선택된 항목들 기반으로 최종 입금 실행
     */
    @Transactional
    public Map<String, Object> executePaymentByItems(Long companyId, List<Long> itemIds, Long sourceAccountId) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new IllegalArgumentException("입금할 항목이 선택되지 않았습니다.");
        }

        // 1. 입금 대상 Item 및 관련 데이터 조회
        List<DcContributionItem> itemsToPay = itemRepo.findAllById(itemIds);
        Set<Long> memberIds = itemsToPay.stream().map(it -> it.getDcMember().getId()).collect(Collectors.toSet());
        Map<Long, String> latestStatusMap = toMap(statusRepo.findLatestStatusPairs(memberIds));

        long finalTotalAmount = 0;
        List<DcContributionItem> validatedItems = new ArrayList<>();

        // 2. 실행 직전 최종 유효성 검증 및 금액 계산
        for (DcContributionItem item : itemsToPay) {
            // 소유권 확인
            if (!item.getBatch().getCompany().getId().equals(companyId)) {
                throw new IllegalStateException("타 회사의 납입 항목이 포함되어 있습니다. (항목 ID: " + item.getId() + ")");
            }
            // 최종 상태 확인
            String currentStatus = latestStatusMap.getOrDefault(item.getDcMember().getId(), "UNKNOWN");
            if ("재직".equals(currentStatus) || "ACTIVE".equalsIgnoreCase(currentStatus)) {
                validatedItems.add(item);
                finalTotalAmount += item.getAmount();
            } else {
                 log.warn("입금 실행 제외: 회원 ID {}의 상태가 변경됨 (실행시: {})", item.getDcMember().getId(), currentStatus);
            }
        }
        
        if (validatedItems.isEmpty()) {
            throw new IllegalStateException("최종 납입 대상이 없습니다 (상태 변경 등으로 모두 제외됨).");
        }

        // 3. 회사 계좌 출금
        CompanyAccount src = companyAccountRepository.findByIdAndCompanyId(sourceAccountId, companyId)
                .orElseThrow(() -> new IllegalStateException("출금계좌를 찾을 수 없습니다."));
        long srcBalance = parseLongSafe(src.getBalance());
        if (srcBalance < finalTotalAmount) {
            throw new IllegalStateException("출금계좌 잔액이 부족합니다.");
        }
        src.setBalance(Long.toString(srcBalance - finalTotalAmount));

        // 4. 가입자 DC 계좌 입금 및 이력 기록
        LocalDateTime now = LocalDateTime.now();
        Set<DcContributionBatch> affectedBatches = new HashSet<>();
        for (DcContributionItem it : validatedItems) {
            DcMember m = it.getDcMember();
            DcAccount dest = accountRepo.findByDcMember_Id(m.getId())
                    .orElseThrow(() -> new IllegalStateException("DC계좌 없음: " + m.getName()));
            
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
            affectedBatches.add(it.getBatch());
            
            // Item의 상태도 EXECUTED로 변경 (별도 상태 필드가 있다면)
            // it.setExecutionStatus(ExecutionStatus.SUCCESS); 

        }
        
        updateBatchStatusIfCompleted(affectedBatches);


        // 5. 결과 반환
        return Map.of("processedCount", validatedItems.size(),
                      "totalPaidAmount", finalTotalAmount,
                      "paidAt", now.toString());
    }
    
    /**
     * [신규 추가] 영향을 받은 Batch들의 모든 Item이 처리 완료되었는지 확인하고 상태를 업데이트하는 메소드
     */
    private void updateBatchStatusIfCompleted(Set<DcContributionBatch> batches) {
        for (DcContributionBatch batch : batches) {
            // 해당 배치의 모든 '성공(VALIDATED)' 판정 항목들의 개수
            long totalSuccessItemsInBatch = batch.getItems().stream()
                                                 .filter(i -> i.getValidationStatus() == DcContributionItem.ValidationStatus.SUCCESS)
                                                 .count();
            
            // 해당 배치에 대해 실제로 입금 처리된 내역(History)의 개수
            long paidItemsInBatch = dcDepositHistoryRepository.countByItem_Batch(batch);

            // 두 숫자가 같으면 모든 건이 처리된 것이므로, Batch의 상태를 EXECUTED로 변경
            if (totalSuccessItemsInBatch > 0 && totalSuccessItemsInBatch == paidItemsInBatch) {
                batch.setStatus(DcContributionBatch.BatchStatus.EXECUTED);
            }
        }
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