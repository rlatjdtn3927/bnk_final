// src/main/java/com/example/memo/purchase/service/PendingBuyService.java
package com.example.memo.purchase.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.repository.purchase.analysis.FundNavRepository;
import com.example.memo.jpa.repository.purchase.commodity.FundMasterRepository;
import com.example.memo.jpa.repository.purchase.commodity.PrincipalGuaranteeRepository;
import com.example.memo.jpa.repository.purchase.portfolio.TransactionHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.RequiredArgsConstructor;

/**
 * FLOW: 매수예정 변경내역 화면
 * - 계좌의 전체 거래내역을 최신순으로 뽑아와서
 *   상품명/평가금액(수량*현재가격)을 붙여 보여준다.
 * - 상품명: FundMaster/PrincipalGuarantee 에서 productId로 조회
 * - 평가금액: FUND/ETF/TDF는 FundNAV 기준가, 예금은 NAV 없으면 1로 곱(=수량=금액)
 */
@Service
@RequiredArgsConstructor
public class PendingBuyService {

    private final ObjectMapper om;
    private final TransactionHistoryRepository txnRepo;
    private final FundMasterRepository fundRepo;
    private final PrincipalGuaranteeRepository principalRepo;
    private final FundNavRepository fundNavRepo;

    private AccountType parseAccountType(String v){
        return v==null? null : AccountType.valueOf(v.trim().toUpperCase());
    }

    /** FLOW: 매수예정 변경내역 – 계좌 전체 거래 최신순 → 화면에 맞게 가공 */
    public JsonNode history(JsonNode req){
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText(); // String (IRP계좌번호 or DC ID)

        ArrayNode out = om.createArrayNode();

        txnRepo.findByAccountTypeAndAcountIdOrderByTxnDateDesc(accountType, acountId).forEach(tx -> {
            String pid  = tx.getProductId();

            // 상품명 조회: FundMaster → PrincipalGuarantee 순
            String name = fundRepo.findByProductId(pid).map(f -> f.getProductName())
                    .orElseGet(() -> principalRepo.findByProductId(pid).map(p -> p.getProductName()).orElse("-"));

            // 평가금액 = 수량 * NAV (NAV 없으면 1)
            BigDecimal nav = fundNavRepo.findTopByFund_ProductIdOrderByReferenceDateDesc(pid)
                    .map(n -> n.getNav()==null ? BigDecimal.ONE : n.getNav())
                    .orElse(BigDecimal.ONE);
            BigDecimal qty  = tx.getQuantity()==null ? BigDecimal.ZERO : tx.getQuantity();
            BigDecimal eval = qty.multiply(nav);

            // 날짜(LocalDate/LocalDateTime 모두 수용)
            String dateStr;
            Object raw = tx.getTxnDate();
            if (raw instanceof LocalDateTime dt)      dateStr = dt.toLocalDate().toString();
            else if (raw instanceof LocalDate d)      dateStr = d.toString();
            else if (raw != null)                     dateStr = raw.toString();
            else                                      dateStr = "-";

            out.add(om.createObjectNode()
                    .put("txnDate",     dateStr)                                   // 거래일
                    .put("accountType", tx.getAccountType()==null? "" : tx.getAccountType().name())
                    .put("productId",   pid)                                        // 상품ID
                    .put("productName", name)                                       // 상품명
                    .put("evalAmt",     eval.longValue()));                          // 평가금액(원)
        });

        return out;
    }

    // (TODO) 매수예정 목록/업서트는 나중에 확장 – 지금은 스텁 유지
    public JsonNode list(JsonNode req){ return om.createArrayNode(); }
    public JsonNode upsert(JsonNode req){ return om.createObjectNode().put("status","OK"); }
}
