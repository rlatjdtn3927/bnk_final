// src/main/java/com/example/memo/purchase/service/MaturityReservationService.java
package com.example.memo.purchase.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.common.ProductType;
import com.example.memo.jpa.entity.purchase.common.TxnType;
import com.example.memo.jpa.repository.purchase.commodity.PrincipalGuaranteeRepository;
import com.example.memo.jpa.repository.purchase.portfolio.PortfolioRepository;
import com.example.memo.jpa.repository.purchase.portfolio.TransactionHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * FLOW: 만기예정 목록 조회(월별)
 * - 기준: 거래내역(TransactionHistory) 중 예금(PRINCIPAL) 상품의 BUY를 월별로 묶어줌
 * - 응답: [{yearMonth:"2025-03", items:[{productId, productName, bankName, buyDate, amount, rate, accountType}...]}...]
 * - 금리: principal_guarantee에서 IRP/DC 구분하여 제공
 */
@Service
@RequiredArgsConstructor
public class MaturityReservationService {

    private final ObjectMapper om;
    private final PortfolioRepository portfolioRepo;
    private final TransactionHistoryRepository txnRepo;
    private final PrincipalGuaranteeRepository principalRepo;

    /** FLOW: 만기예정 목록(월별) */
    public JsonNode listMonthlyPrincipalContributions(JsonNode req){
        String atStr = req.path("accountType").asText();
        String acId  = req.path("acountId").asText();
        LocalDate from = parseDateOr(req.path("from").asText(null), LocalDate.now().minusMonths(12).withDayOfMonth(1));
        LocalDate to   = parseDateOr(req.path("to").asText(null),   LocalDate.now());

        AccountType at = AccountType.valueOf(atStr);

        // 1) 내가 가진 '예금(PRINCIPAL)' 상품ID 목록
        List<String> principalPids = portfolioRepo
                .findByAccountTypeAndAcountIdAndProductType(at, acId, ProductType.PRINCIPAL)
                .stream().map(p -> p.getProductId()).distinct().toList();
        if (principalPids.isEmpty()) return om.createArrayNode();

        // 2) 해당 상품들의 BUY 거래를 기간 내 조회 → 월별 그룹핑
        List<com.example.memo.jpa.entity.purchase.portfolio.TransactionHistory> buys =
                txnRepo.findByAccountTypeAndAcountIdAndTxnTypeAndProductIdInAndTxnDateBetween(
                        at, acId, TxnType.BUY, principalPids, from, to);

        Map<YearMonth, List<com.example.memo.jpa.entity.purchase.portfolio.TransactionHistory>> byYm =
                buys.stream().collect(Collectors.groupingBy(txn -> YearMonth.from(txn.getTxnDate()),
                        TreeMap::new, Collectors.toList()));

        ArrayNode out = om.createArrayNode();
        for (var entry : byYm.entrySet()){
            YearMonth ym = entry.getKey();
            ArrayNode items = om.createArrayNode();
            for (var t : entry.getValue()){
                String pid = t.getProductId();
                PrincipalGuarantee pg = principalRepo.findById(pid).orElse(null);

                ObjectNode o = om.createObjectNode();
                o.put("productId", pid);
                o.put("bankName",   pg==null ? "" : nz(pg.getBankName()));
                o.put("productName",pg==null ? "" : nz(pg.getProductName()));
                o.put("buyDate",    t.getTxnDate().toString());
                o.put("amount",     t.getTxnAmt()==null ? "0" : t.getTxnAmt().toPlainString());
                // 계좌유형별 금리 표시
                BigDecimal rate = BigDecimal.ZERO;
                if (pg != null){
                    rate = at==AccountType.IRP ? nz(pg.getIrpRate())
                         : at==AccountType.DC  ? nz(pg.getDcRate())
                         : nz(pg.getDbRate());
                }
                o.put("annualRate", rate.toPlainString());
                o.put("accountType", at.name());
                items.add(o);
            }
            ObjectNode group = om.createObjectNode();
            group.put("yearMonth", ym.toString()); // "2025-06"
            group.set("items", items);
            out.add(group);
        }
        return out;
    }

    /** FLOW: 만기 변경 '예약' 저장(스키마 미정이므로 스텁) */
    public JsonNode applyReservation(JsonNode req){
        ObjectNode out = om.createObjectNode();
        out.put("ok", true);
        out.put("status","OK");
        // TODO: 스키마 확정 후 DB 저장/조회로 교체
        return out;
    }

    // ────────────── util ──────────────
    private static String nz(String s){ return s==null? "" : s; }
    private static BigDecimal nz(BigDecimal v){ return v==null? BigDecimal.ZERO : v; }
    private LocalDate parseDateOr(String s, LocalDate def){
        if(s==null || s.isBlank()) return def;
        return LocalDate.parse(s);
    }
}
