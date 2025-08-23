package com.example.memo.company.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.example.memo.jpa.entity.ledger.FundHoldings;
import com.example.memo.jpa.entity.ledger.FundLedger;
import com.example.memo.jpa.entity.ledger.PrincipalLedger;
import com.example.memo.jpa.entity.purchase.analysis.FundNav;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanFund;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanPG;
import com.example.memo.jpa.repository.company.CompanyAccountRepository;
import com.example.memo.jpa.repository.company.CompanyManagerRepository;
import com.example.memo.jpa.repository.company.CompanyRepository;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.company.DcContributionBatchRepository;
import com.example.memo.jpa.repository.company.DcContributionItemRepository;
import com.example.memo.jpa.repository.company.DcDepositHistoryRepository;
import com.example.memo.jpa.repository.company.DcMemberRepository;
import com.example.memo.jpa.repository.company.DcMemberStatusRepository;
import com.example.memo.jpa.repository.ledger.FundHoldingsRepository;
import com.example.memo.jpa.repository.ledger.FundLedgerRepository;
import com.example.memo.jpa.repository.ledger.PrincipalLedgerRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundNavRepository;
import com.example.memo.jpa.repository.purchase.commodity.FundMasterRepository;
import com.example.memo.jpa.repository.purchase.trade.BuyPlanFundRepository;
import com.example.memo.jpa.repository.purchase.trade.BuyPlanPGRepository;
import com.example.memo.ocr.client.ClovaOcrClient;
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
	
	static final int SCALE_CAL = 12;
	static final int SCALE_SAVE = 6;
	static final int SCALE_RATE = 4;
	static final RoundingMode RMUP = RoundingMode.HALF_UP;
	static final RoundingMode RMDN = RoundingMode.DOWN;
	
    private final DcContributionBatchRepository batchRepo;
    private final DcDepositHistoryRepository dcDepositHistoryRepository;
    private final DcMemberRepository dcMemberRepo;
    private final DcMemberStatusRepository statusRepo;
    private final DcAccountRepository accountRepo;
    private final CompanyRepository companyRepo;
    private final CompanyManagerRepository managerRepo;
    private final CompanyAccountRepository companyAccountRepository;
    private final DcContributionItemRepository itemRepo;
    private final BuyPlanFundRepository buyPlanFundRepository;
    private final BuyPlanPGRepository buyPlanPGRepository;
    private final FundLedgerRepository fundLedgerRepository;
    private final FundHoldingsRepository fundHoldingsRepository;
    private final PrincipalLedgerRepository principalLedgerRepository;
    private final FundNavRepository fundNavRepository;
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
     // 기존 코드 일부
        for (JsonNode n : itemNodes) {
            String ssn   = text(n, "ssn");
            long amount  = parseLong(text(n, "amount"));
            long bonus   = parseLong(text(n, "performanceBonus"));
            long money   = amount + bonus;
            totalAmount += money;

            DcMember member = null;
            if (ssn != null && !ssn.isBlank()) {

                // ① 숫자 13자리로 정규화
                String d13 = normalizeDigits13(ssn);

                if (d13 != null && !d13.isBlank()) {
                    // ② 표준(하이픈 없음) 암호문으로 1차 조회
                    String encStd = cryptoService.encrypt(d13);
                    member = dcMemberRepo.findByRrn(encStd).orElse(null);

                    // ③ 레거시(하이픈 포함 저장)까지 커버하려면 듀얼 조회
                    if (member == null) {
                        String legacy = d13.substring(0, 6) + "-" + d13.substring(6);
                        String encLegacy = cryptoService.encrypt(legacy);
                        member = dcMemberRepo.findByRrn(encLegacy).orElse(null);
                    }
                }
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
        long okSum = 0L;
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
                if (errorMessage == null && !accountActiveMap.getOrDefault(m.getId(), false)) {
                    errorMessage = "DC 계좌가 없거나 비활성 상태입니다.";
                }
                if (errorMessage == null) {
                    String status = statusMap.getOrDefault(m.getId(), "UNKNOWN");
                    if (!("재직".equals(status) || "ACTIVE".equalsIgnoreCase(status))) {
                        errorMessage = "납입 대상 상태가 아닙니다(현재상태: " + status + ").";
                    }
                }
                if (errorMessage == null) {
                    long annualSalary = annualSalaryMap.getOrDefault(m.getId(), 0L);
                    if (annualSalary <= 0) {
                        errorMessage = "연간임금총액(annualSalary)이 설정되지 않았습니다.";
                    } else {
                        long monthlyMin = annualSalary / 144;
                        long amount = Optional.ofNullable(it.getAmount()).orElse(0L);
                        if (amount < monthlyMin) {
                            errorMessage = "월 최소 납입금액 미만입니다. (최소: " + String.format("%,d", monthlyMin) + "원)";
                        }
                    }
                }
                if (errorMessage == null && (it.getAmount() == null || it.getAmount() <= 0)) {
                    errorMessage = "납입금액은 0보다 커야 합니다.";
                }
            }

            boolean isSuccess = (errorMessage == null);
            if (isSuccess) {
                ok++;
                okSum += Optional.ofNullable(it.getAmount()).orElse(0L);
                it.setValidationStatus(DcContributionItem.ValidationStatus.SUCCESS);
            } else {
                err++;
                it.setValidationStatus(DcContributionItem.ValidationStatus.FAIL);
            }
            it.setErrorMessage(errorMessage);

            itemDtos.add(ContribValidationItemDto.builder()
                    .itemId(it.getId())
                    .amount(it.getAmount() == null ? 0L : it.getAmount())
                    .validationStatus(isSuccess ? "OK" : "FAIL")
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
                .totalOkAmount(okSum)
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

        // 성공 항목만 입금대기로 전환
        int pendingCount = 0;
        for (DcContributionItem it : batch.getItems()) {
            if (it.getValidationStatus() == DcContributionItem.ValidationStatus.SUCCESS) {
                it.setPaymentStatus(DcContributionItem.PaymentStatus.PENDING);
                pendingCount++;
            }
        }

        // 배치 확정
        batch.setStatus(DcContributionBatch.BatchStatus.CONFIRMED);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("batchId", batch.getId());
        resp.put("status", batch.getStatus().name());
        resp.put("okCount", Optional.ofNullable(batch.getOkCount()).orElse(0));
        resp.put("pendingCount", pendingCount);
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
     * 개별 납입 예정 항목 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PayableItemDto> listPayableItems(Long companyId, LocalDate from, LocalDate to, String paymentStatus, Pageable pageable) {

        // 1. 컨트롤러에서 어떤 문자열을 받았는지 확인
    	System.out.println("SERVICE_LOG_1: Controller에서 받은 paymentStatus 문자열: '" + paymentStatus + "'");

        List<DcContributionItem.PaymentStatus> statusesToSearch;
        
        if (paymentStatus != null && !"ALL".equalsIgnoreCase(paymentStatus)) {
            statusesToSearch = List.of(DcContributionItem.PaymentStatus.valueOf(paymentStatus.toUpperCase()));
        } else {
            statusesToSearch = Arrays.asList(
                DcContributionItem.PaymentStatus.PENDING, 
                DcContributionItem.PaymentStatus.PAID, 
                DcContributionItem.PaymentStatus.SKIPPED,
                DcContributionItem.PaymentStatus.FAILED
            );
        }
        
        // 2. Repository로 어떤 Enum 리스트를 보낼 것인지 확인
    	System.out.println("SERVICE_LOG_2: Repository로 보낼 statusesToSearch 리스트: {}" +  statusesToSearch);
        return itemRepo.findPayableItemsWithStatus(
                companyId,
                from,
                to,
                statusesToSearch,
                pageable
        );
    }
    
    /**
     * 선택된 항목들 기반으로 최종 입금 실행
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
        // 영향을 받는 배치를 추적하기 위해 Set을 사용 (중복 방지)
        Set<DcContributionBatch> affectedBatches = new HashSet<>();

        // 2. 실행 직전 최종 유효성 검증 및 금액 계산
        for (DcContributionItem item : itemsToPay) {
            // 소유권 확인
            if (!item.getBatch().getCompany().getId().equals(companyId)) {
                throw new IllegalStateException("타 회사의 납입 항목이 포함되어 있습니다. (항목 ID: " + item.getId() + ")");
            }
            
            // 이미 처리된 건은 건너뜀 (예: PAID, SKIPPED)
            if (item.getPaymentStatus() != null && item.getPaymentStatus() != DcContributionItem.PaymentStatus.PENDING) {
                log.warn("이미 처리된 항목이므로 건너뜁니다. (항목 ID: {}, 상태: {})", item.getId(), item.getPaymentStatus());
                continue;
            }
            
            // 최종 상태 확인
            String currentStatus = latestStatusMap.getOrDefault(item.getDcMember().getId(), "UNKNOWN");
            if ("재직".equals(currentStatus) || "ACTIVE".equalsIgnoreCase(currentStatus)) {
                validatedItems.add(item);
                finalTotalAmount += item.getAmount();
            } else {
                // 입금 제외된 항목의 상태를 SKIPPED로 변경
                item.setPaymentStatus(DcContributionItem.PaymentStatus.SKIPPED);
                log.warn("입금 실행 제외: 회원 ID {}의 상태가 변경됨 (실행시: {})", item.getDcMember().getId(), currentStatus);
            }
            // 검증 과정에서 연관된 모든 배치를 추가
            affectedBatches.add(item.getBatch());
        }
        
        if (validatedItems.isEmpty()) {
            // 모든 항목이 제외되었더라도, SKIPPED 상태는 저장되어야 하므로 배치를 업데이트
            updateBatchStatusIfCompleted(affectedBatches);
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
        for (DcContributionItem it : validatedItems) {
            DcMember m = it.getDcMember();
            DcAccount dest = accountRepo.findByDcMember_Id(m.getId())
                    .orElseThrow(() -> new IllegalStateException("DC계좌 없음: " + m.getName()));
            
            BigDecimal dBal = Optional.ofNullable(dest.getBalance()).orElse(BigDecimal.ZERO);
            BigDecimal deposited = BigDecimal.valueOf(it.getAmount());
            BigDecimal temp = BigDecimal.valueOf(it.getAmount());
            
            /**매수 예정 등록을 확인해보고 있으면 예정된 품목 구매 아니면 그냥 계좌로 입금**/
            List<BuyPlanFund> buyPlanList = buyPlanFundRepository.findByDcAccountAndIsCurrent(dest, "Y");
            if(!buyPlanList.isEmpty()) { //매수 예정 등록분이 있다면 거래 진행
            	for(BuyPlanFund plan : buyPlanList) {
            		
            		Integer ratio = plan.getAllocationPercent();
            		BigDecimal tradeAmt = temp.multiply(BigDecimal.valueOf(ratio)).divide(BigDecimal.valueOf(100), SCALE_CAL, RMDN);
            		
            		FundMaster fund = plan.getFund(); 
            		String prodId = fund.getProductId();
            		FundNav fundNav = fundNavRepository.findTopByFund_ProductIdOrderByReferenceDateDesc(prodId).orElseThrow();
            		BigDecimal nav = fundNav.getNav();
            		BigDecimal addUnits = tradeAmt.divide(nav, SCALE_CAL, RMDN);
            		
            		deposited = deposited.subtract(tradeAmt);
            		
            		fundLedgerRepository.save(FundLedger.builder()
            				.dcAccount(dest)
            				.fund(fund)
            				.tradeType("BUY")
            				.tradeUnits(addUnits.setScale(SCALE_SAVE, RMDN))
            				.tradePrice(nav.setScale(SCALE_SAVE, RMDN))
            				.tradeAmount(tradeAmt.setScale(SCALE_SAVE, RMDN))
            				.build());
            		
            		FundHoldings fundholdings = fundHoldingsRepository.findByDcAccountAndFund(dest, fund);
            		if(fundholdings != null) { //있으면 업데이트 진행
            			BigDecimal oldUnits = fundholdings.getUnits();
            			BigDecimal newUnits = oldUnits.add(addUnits); // 새로운 보유좌수
            			BigDecimal acquisitionAmount = fundholdings.getAcquisitionAmount();
            			BigDecimal newAvgPrice = acquisitionAmount.add(tradeAmt).divide(newUnits, SCALE_CAL, RMDN); // 새로운 평균단가
            			BigDecimal newAcquisitionAmount = newAvgPrice.multiply(newUnits); //새로운 매수원금
            			BigDecimal newValuationAmt = newUnits.multiply(nav); //새로운 평가액
            			BigDecimal newProfitLoss = newValuationAmt.subtract(newAcquisitionAmount); //새로운 평가손익
			    		BigDecimal newRr = newAcquisitionAmount.signum()==0 ? BigDecimal.ZERO : // 새로운 수익률 산출
			    		    newProfitLoss.multiply(BigDecimal.valueOf(100))
			    		                 .divide(newAcquisitionAmount, SCALE_RATE, RMUP);
			    		
			    		fundholdings.setUnits(newUnits.setScale(SCALE_SAVE, RMDN));
			    		fundholdings.setAvgPrice(newAvgPrice.setScale(SCALE_SAVE, RMDN));
			    		fundholdings.setAcquisitionAmount(newAcquisitionAmount.setScale(SCALE_SAVE, RMDN));
			    		fundholdings.setValuationAmount(newValuationAmt.setScale(SCALE_SAVE, RMDN));
			    		fundholdings.setProfitLoss(newProfitLoss.setScale(SCALE_SAVE, RMDN));
			    		fundholdings.setReturnRate(newRr.setScale(SCALE_RATE, RMUP));
			    		
			    		fundHoldingsRepository.save(fundholdings);
            		} else { //없으면 새로 삽입
            			fundHoldingsRepository.save(FundHoldings.builder()
            					.units(addUnits.setScale(SCALE_SAVE, RMDN))
            					.dcAccount(dest)
            					.fund(fund)
            					.avgPrice(nav)
            					.acquisitionAmount(tradeAmt)
            					.valuationAmount(tradeAmt)
            					.profitLoss(BigDecimal.ZERO)
            					.returnRate(BigDecimal.ZERO)
            					.build());
            		}
            	}
            }
            
    		List<BuyPlanPG> buyPlanPgList = buyPlanPGRepository.findByDcAccountAndIsCurrent(dest, "Y");
    		if(buyPlanPgList.isEmpty()) {
    			for(BuyPlanPG buyPlan : buyPlanPgList) {
    				Integer ratio = buyPlan.getAllocationPercent();
    				BigDecimal tradeAmt = temp.multiply(BigDecimal.valueOf(ratio)).divide(BigDecimal.valueOf(100), SCALE_CAL, RMDN);
    				
    				deposited = deposited.subtract(tradeAmt);
    				
    				PrincipalGuarantee pg = buyPlan.getPrincipal();
    				long years = 0L;
    				switch (pg.getMaturityYears()) {
        				case "1년" -> years = 1L;
        				case "2년" -> years = 2L;
        				case "3년" -> years = 3L;
        				case "5년" -> years = 5L;
    				}
    				principalLedgerRepository.save(PrincipalLedger.builder()
    						.dcAccount(dest)
    						.principal(pg)
    						.contractAmount(tradeAmt.setScale(SCALE_SAVE, RMDN))
    						.interestRate(pg.getDcRate())
    						.startDate(LocalDate.now())
    						.maturityDate(LocalDate.now().plusYears(years))
    						.interestAccrued(BigDecimal.ZERO)
    						.status("ACTIVE")
    						.build());
    			}
    		}
            	
            dest.setBalance(dBal.add(deposited));

            DcDepositHistory h = DcDepositHistory.builder()
                    .item(it)
                    .sourceAccount(src)
                    .destinationAccount(dest)
                    .paidAmount(it.getAmount())
                    .paidAt(now)
                    .build();
            dcDepositHistoryRepository.save(h);
            
            // 입금 처리된 항목의 상태를 PAID로 변경
            it.setPaymentStatus(DcContributionItem.PaymentStatus.PAID);
            
            // 배치가 '확정' 상태일 경우 '처리중'으로 변경
            DcContributionBatch batch = it.getBatch();
            if (batch.getStatus() == DcContributionBatch.BatchStatus.CONFIRMED) {
                batch.setStatus(DcContributionBatch.BatchStatus.PROCESSING);
            }
        }
        
        // 모든 처리가 끝난 후, 영향을 받은 배치들의 최종 상태를 업데이트
        updateBatchStatusIfCompleted(affectedBatches);

        // 5. 결과 반환
        return Map.of("processedCount", validatedItems.size(),
                      "totalPaidAmount", finalTotalAmount,
                      "paidAt", now.toString());
    }
    
    /**
     * 영향을 받은 Batch들의 모든 Item이 처리 완료되었는지 확인하고 상태를 업데이트하는 메소드
     */
    private void updateBatchStatusIfCompleted(Set<DcContributionBatch> batches) {
        for (DcContributionBatch batch : batches) {
            // 해당 배치의 모든 '성공(SUCCESS)' 판정 항목들을 가져옴
            List<DcContributionItem> payableItems = batch.getItems().stream()
                    .filter(i -> i.getValidationStatus() == DcContributionItem.ValidationStatus.SUCCESS)
                    .toList();

            if (payableItems.isEmpty()) {
                continue; // 처리할 항목이 없으면 건너뜀
            }
            
            // 모든 성공 항목이 '입금완료(PAID)' 또는 '입금제외(SKIPPED)' 상태인지 확인
            boolean allItemsProcessed = payableItems.stream()
                    .allMatch(item ->
                            item.getPaymentStatus() == DcContributionItem.PaymentStatus.PAID ||
                            item.getPaymentStatus() == DcContributionItem.PaymentStatus.SKIPPED
                    );

            // 모든 건이 처리되었다면, Batch의 상태를 EXECUTED로 변경
            if (allItemsProcessed) {
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
    
    // 유틸: 서비스 내부 private 메서드로 추가
    private String normalizeDigits13(String rrnRaw) {
        if (rrnRaw == null) return null;
        String d = rrnRaw.replaceAll("[^0-9]", "").trim();
        if (!d.isEmpty() && d.length() != 13) {
            throw new IllegalArgumentException("잘못된 주민등록번호 형식: " + rrnRaw);
        }
        return d;
    }

}