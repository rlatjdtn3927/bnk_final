package com.example.memo.company.handler;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.example.memo.company.dto.ContribPlanListResponse;
import com.example.memo.company.dto.ContribValidationResultDto;
import com.example.memo.company.dto.PayableItemDto;
import com.example.memo.company.service.DcContributionService;
import com.example.memo.jpa.entity.company.CompanyAccount;
import com.example.memo.jpa.entity.company.DcContributionBatch;
import com.example.memo.jpa.repository.company.CompanyAccountRepository;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DcContributionHandler implements TcpMessageHandler {

    private final DcContributionService service;
    private final ObjectMapper objectMapper;
    
    private final CompanyAccountRepository companyAccountRepository;


    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("CONTRIBUTION_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            switch (command) {
                case CONTRIBUTION_BATCH_CREATE: {
                    Long batchId = service.createContributionBatch(data);
                    return Map.of("batchId", batchId);
                }
                case CONTRIBUTION_BATCH_VALIDATE: {
                    Long batchId = data.get("batchId").asLong();
                    ContribValidationResultDto result = service.validateBatch(batchId);
                    return objectMapper.convertValue(result, JsonNode.class);
                }
                case CONTRIBUTION_BATCH_CONFIRM: {
                    Long batchId = data.get("batchId").asLong();
                    Long companyId = data.get("companyId").asLong(); // WAS에서 세션 회사ID 전달
                    Map<String, Object> result = service.confirmBatch(batchId, companyId);
                    return objectMapper.convertValue(result, JsonNode.class);
                }
                case CONTRIBUTION_BATCH_LIST: {
                    Long companyId = data.get("companyId").asLong();
                    LocalDate from = data.hasNonNull("fromDate") ? LocalDate.parse(data.get("fromDate").asText()) : null;
                    LocalDate to   = data.hasNonNull("toDate")   ? LocalDate.parse(data.get("toDate").asText())   : null;
                    String statusStr = data.hasNonNull("status") ? data.get("status").asText() : null;
                    DcContributionBatch.BatchStatus status = (statusStr==null||statusStr.isBlank()) ? null
                            : DcContributionBatch.BatchStatus.valueOf(statusStr);
                    String keyword = data.hasNonNull("keyword") ? data.get("keyword").asText() : null;
                    int page = data.hasNonNull("page") ? data.get("page").asInt() : 0;
                    int size = data.hasNonNull("size") ? data.get("size").asInt() : 20;

                    ContribPlanListResponse resp = service.listBatches(companyId, from, to, status, keyword, page, size);
                    return objectMapper.convertValue(resp, JsonNode.class);
                }
                case CONTRIBUTION_COMPANY_ACCOUNT_LIST: {
                    Long companyId = data.get("companyId").asLong();
                    List<CompanyAccount> list = companyAccountRepository.findByCompanyId(companyId);

                    List<Map<String, Object>> rows = list.stream()
                            .map(a -> {
                                long bal = parseLongSafe(a.getBalance()); // String → long
                                Map<String,Object> m = new LinkedHashMap<>();
                                m.put("accountId", a.getId());
                                m.put("accountNo", a.getAccountNo());
                                m.put("balance", bal);
                                return m;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    return objectMapper.convertValue(Map.of("rows", rows), JsonNode.class);
                }
                
                case CONTRIBUTION_BATCH_EXECUTE: {
                    // WAS에서 넘어오는 값: batchId, companyId(세션에서 넣어줌), sourceAccountId
                    if (!data.hasNonNull("batchId") || !data.hasNonNull("companyId") || !data.hasNonNull("sourceAccountId")) {
                        return Map.of("error", "필수 파라미터가 누락되었습니다. (batchId, companyId, sourceAccountId)");
                    }

                    Long batchId = data.get("batchId").asLong();
                    Long companyId = data.get("companyId").asLong();
                    Long sourceAccountId = data.get("sourceAccountId").asLong();

                    Map<String, Object> result = service.executeBatch(batchId, companyId, sourceAccountId);
                    return objectMapper.convertValue(result, JsonNode.class);
                }
                case CONTRIBUTION_PAYABLE_ITEM_LIST: {
                    Long companyId = data.get("companyId").asLong();
                    LocalDate from = data.hasNonNull("fromDate") ? LocalDate.parse(data.get("fromDate").asText()) : LocalDate.now().minusMonths(1);
                    LocalDate to   = data.hasNonNull("toDate")   ? LocalDate.parse(data.get("toDate").asText())   : LocalDate.now();
                    
                    List<DcContributionBatch.BatchStatus> statuses = null;
                    if (data.hasNonNull("statuses")) {
                        // JSON 배열을 Java List로 변환
                        statuses = objectMapper.convertValue(data.get("statuses"), new TypeReference<List<DcContributionBatch.BatchStatus>>() {});
                    }

                    int page = data.hasNonNull("page") ? data.get("page").asInt() : 0;
                    int size = data.hasNonNull("size") ? data.get("size").asInt() : 20;
                    Pageable pageable = PageRequest.of(page, size);

                    Page<PayableItemDto> resultPage = service.listPayableItems(companyId, statuses, from, to, pageable);
                    
                    // Page 객체를 클라이언트에게 보내기 쉬운 Map 형태로 변환
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("items", resultPage.getContent());
                    response.put("page", resultPage.getNumber());
                    response.put("size", resultPage.getSize());
                    response.put("totalElements", resultPage.getTotalElements());
                    response.put("totalPages", resultPage.getTotalPages());

                    return objectMapper.convertValue(response, JsonNode.class);
                }
                
                case CONTRIBUTION_EXECUTE_ITEMS: {
                    Long companyId = data.get("companyId").asLong();
                    Long sourceAccountId = data.get("sourceAccountId").asLong();
                    List<Long> itemIds = objectMapper.convertValue(
                        data.get("itemIds"), 
                        new TypeReference<List<Long>>() {}
                    );

                    Map<String, Object> result = service.executePaymentByItems(companyId, itemIds, sourceAccountId);
                    return objectMapper.convertValue(result, JsonNode.class);
                }
                default:
                    return Map.of("error", "Unsupported command");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    // 숫자/콤마/공백 섞여도 안전하게 파싱
    private long parseLongSafe(String v) {
        if (v == null) return 0L;
        String s = v.replaceAll("[^0-9\\-]", "");
        if (s.isEmpty() || "-".equals(s)) return 0L;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }
}