// src/main/java/com/example/memo/purchase/service/MaturityReservationService.java
package com.example.memo.purchase.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.common.ProductType;
import com.example.memo.jpa.entity.purchase.common.TxnType;
import com.example.memo.jpa.entity.purchase.portfolio.Portfolio;
import com.example.memo.jpa.entity.purchase.portfolio.TransactionHistory;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundNavRepository;
import com.example.memo.jpa.repository.purchase.commodity.PrincipalGuaranteeRepository;
import com.example.memo.jpa.repository.purchase.portfolio.PortfolioRepository;
import com.example.memo.jpa.repository.purchase.portfolio.TransactionHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * FLOW:
 * - [만기예정 목록 조회]   : 계좌의 '예금(PRINCIPAL)' 보유분을 월/상품 단위로 내려준다.
 * - [만기예정 변경예약 적용] : 전환일까지 일할이자 합산 → 전환일에 예금 SELL → 목표배분대로 BUY → CASH는 잔액.
 */
@Service
@RequiredArgsConstructor
public class MaturityReservationService {

    private final ObjectMapper om;

    private final PortfolioRepository portfolioRepo;
    private final PrincipalGuaranteeRepository principalRepo;
    private final TransactionHistoryRepository txnRepo;
    private final FundNavRepository fundNavRepo;
    private final IrpAccountRepository irpRepo;
    private final DcAccountRepository dcRepo;

    // ----- 공통 유틸 -----
    private static BigDecimal nz(BigDecimal v){ return v==null? BigDecimal.ZERO : v; }
    private static BigDecimal pct(int x){ return BigDecimal.valueOf(x).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP); }
    private AccountType parseAccountType(String v){ return v==null? null : AccountType.valueOf(v.trim().toUpperCase()); }
    private ProductType parseProductTypeOrNull(String v){
        if(v==null) return null;
        String x = v.trim().toUpperCase();
        if("CASH".equals(x)) return null; // CASH는 상품이 아님(잔액)
        return ProductType.valueOf(x);
    }
    private BigDecimal pickRateByAccount(PrincipalGuarantee pg, AccountType t){
        if(pg==null) return BigDecimal.ZERO;
        return switch (t){
            case IRP -> nz(pg.getIrpRate());
            case DC  -> nz(pg.getDcRate());
            case DB  -> nz(pg.getDbRate());
        };
    }
    private LocalDate addMaturity(LocalDate start, String maturityYears){
        // 간단 파서: "1년"/"6개월"/"12M"/"2Y" 등
        if(start==null) return null;
        String s = maturityYears==null? "" : maturityYears.trim().toUpperCase();
        if(s.contains("년") || s.endsWith("Y")){
            int y = s.replace("년","").replace("Y","").trim().isEmpty()? 1
                    : Integer.parseInt(s.replace("년","").replace("Y","").trim());
            return start.plusYears(y);
        }else{
            String n = s.replace("개월","").replace("M","").trim();
            int m = n.isEmpty()? 6 : Integer.parseInt(n);
            return start.plusMonths(m);
        }
    }

    // ------------------------------------------------------------------------
    // [만기예정 목록 조회]
    // 요청:  { accountType, acountId }
    // 응답:  [{productId, productName, rate, startDate, maturityDate, principal, monthKey}, ...]
    // 설명:  '예금(PRINCIPAL)' 보유분만 뽑아 계좌유형별 금리/시작일/예상만기일을 붙여준다.
    // ------------------------------------------------------------------------
    public JsonNode list(JsonNode req){
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText();

        ArrayNode out = om.createArrayNode();

        // 예금 보유분만 조회
        List<Portfolio> principals = portfolioRepo.findByAccountTypeAndAcountIdAndProductType(
                accountType, acountId, ProductType.PRINCIPAL);

        for(Portfolio p : principals){
            String pid = p.getProductId();
            Optional<PrincipalGuarantee> pgOpt = principalRepo.findByProductId(pid);
            PrincipalGuarantee pg = pgOpt.orElse(null);

            BigDecimal rate = pickRateByAccount(pg, accountType); // 계좌유형별 금리
            LocalDate start = p.getStartDate();                   // 최초 편입일
            LocalDate maturity = addMaturity(start, pg==null? null : pg.getMaturityYears());

            ObjectNode line = om.createObjectNode();
            line.put("productId",    pid);
            line.put("productName",  pg==null? "-" : pg.getProductName());
            line.put("rate",         rate.toPlainString());
            line.put("startDate",    start==null? "" : start.toString());
            line.put("maturityDate", maturity==null? "" : maturity.toString());
            line.put("principal",    nz(p.getEvalAmt()).longValue());   // 보유 평가액(예금 잔액)
            line.put("monthKey",     (start==null? "" : start.withDayOfMonth(1).toString()));
            out.add(line);
        }
        return out;
    }

    // ------------------------------------------------------------------------
    // [만기예정 변경예약 적용]
    // 요청:
    // {
    //   "accountType":"IRP|DC",
    //   "acountId":"...",
    //   "productId":"예금 상품ID(전환대상)",
    //   "changeDate":"YYYY-MM-DD",
    //   "items":[ {"productType":"FUND|ETF|TDF|PRINCIPAL|CASH","productId":"...","ratio":30}, ... ]
    // }
    //
    // 처리:
    // 1) 전환일 기준 해당 예금의 BUY(납입) 내역 조회
    // 2) 각 납입금액에 대해 (납입일~전환일) 일할이자 합산(원단위 절사)
    // 3) 전환일에 원금+이자(총액) 예금 SELL
    // 4) 총액을 목표배분대로 BUY (FUND/ETF/TDF는 최신 NAV로 수량 산정, CASH는 잔액)
    // 응답: {status, changedAt, sellAmt, interest, buyBreakdown:[...], cashLeft, portfolio:[...] }
    // ------------------------------------------------------------------------
    @Transactional
    public JsonNode apply(JsonNode req){
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText();
        String productId        = req.path("productId").asText();     // 예금 상품
        LocalDate changeDate    = LocalDate.parse(req.path("changeDate").asText());

        // 0) 예금 보유/상품확인
        Portfolio principalP = portfolioRepo
                .findByAccountTypeAndAcountIdAndProductTypeAndProductId(accountType, acountId, ProductType.PRINCIPAL, productId)
                .orElseThrow(() -> new IllegalArgumentException("예금 보유 내역이 없습니다."));
        PrincipalGuarantee pg = principalRepo.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("예금 상품 마스터가 없습니다."));

        // 1) 전환일까지 BUY 내역(리포지토리 시그니처에 맞는 파라미터 순서!)
        List<TransactionHistory> buys = txnRepo
                .findByAccountTypeAndAcountIdAndTxnTypeAndProductIdAndTxnDateLessThanEqual(
                        accountType, acountId, TxnType.BUY, productId, changeDate);

        BigDecimal annualRate   = pickRateByAccount(pg, accountType).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal principalSum = BigDecimal.ZERO;
        BigDecimal interestSum  = BigDecimal.ZERO;

        for(TransactionHistory t : buys){
            BigDecimal amt = nz(t.getTxnAmt());         // 납입금액
            LocalDate from = t.getTxnDate();            // 납입일
            long days = Math.max(0, ChronoUnit.DAYS.between(from, changeDate));
            BigDecimal interest = amt
                    .multiply(annualRate)
                    .multiply(BigDecimal.valueOf(days))
                    .divide(BigDecimal.valueOf(365), 0, RoundingMode.DOWN); // 원단위 절사
            principalSum = principalSum.add(amt);
            interestSum  = interestSum.add(interest);
        }

        BigDecimal proceed = principalSum.add(interestSum); // 전환일 회수 총액(원금+이자)

        // 2) 전환일에 예금 SELL (보유량/투자액/평가액 0)
        principalP.setQuantity(BigDecimal.ZERO);
        principalP.setInvestAmt(BigDecimal.ZERO);
        principalP.setEvalAmt(BigDecimal.ZERO);
        portfolioRepo.save(principalP);

        txnRepo.save(TransactionHistory.builder()
                .accountType(accountType)
                .acountId(acountId)
                .productId(productId)
                .txnType(TxnType.SELL)
                .txnAmt(proceed)
                .quantity(proceed)                // 예금은 1:1
                .txnDate(changeDate)
                .build());

        // 3) 목표배분 비율 검증(합 100%)
        int sum=0; for(JsonNode it : req.withArray("items")) sum+= it.path("ratio").asInt();
        if(sum!=100) return om.createObjectNode().put("error","배분 비율 합계가 100%가 아닙니다.");

        // 4) 전환금액을 목표배분대로 BUY, CASH는 잔액
        ArrayNode buyBreakdown = om.createArrayNode();
        int cashRatio = 0;

        for(JsonNode it : req.withArray("items")){
            String typeStr = it.path("productType").asText();
            ProductType type = parseProductTypeOrNull(typeStr);
            String pid  = it.path("productId").asText(null);
            int ratio   = it.path("ratio").asInt();

            BigDecimal amount = proceed.multiply(pct(ratio)).setScale(0, RoundingMode.DOWN);

            if(type == null){ // CASH
                cashRatio += ratio;
                ObjectNode line = om.createObjectNode();
                line.put("productType","CASH");
                line.put("amount", amount.longValue());
                buyBreakdown.add(line);
                continue;
            }

            BigDecimal qty;
            if(type==ProductType.FUND || type==ProductType.ETF || type==ProductType.TDF){
                BigDecimal nav = fundNavRepo.findTopByFund_ProductIdOrderByReferenceDateDesc(pid)
                        .map(n -> nz(n.getNav())).filter(n -> n.signum()>0).orElse(BigDecimal.ONE);
                qty = amount.divide(nav, 2, RoundingMode.DOWN);
            }else{ // 다시 예금으로 전환하는 경우
                qty = amount; // 1:1
            }

            // upsert 포트폴리오
            Portfolio p = portfolioRepo
                    .findByAccountTypeAndAcountIdAndProductTypeAndProductId(accountType, acountId, type, pid)
                    .orElseGet(() -> Portfolio.builder()
                            .accountType(accountType)
                            .acountId(acountId)
                            .productType(type)
                            .productId(pid)
                            .quantity(BigDecimal.ZERO)
                            .investAmt(BigDecimal.ZERO)
                            .evalAmt(BigDecimal.ZERO)
                            .startDate(changeDate)
                            .build());

            p.setQuantity(nz(p.getQuantity()).add(qty));
            p.setInvestAmt(nz(p.getInvestAmt()).add(amount));
            p.setEvalAmt(nz(p.getEvalAmt()).add(amount));
            portfolioRepo.save(p);

            txnRepo.save(TransactionHistory.builder()
                    .accountType(accountType)
                    .acountId(acountId)
                    .productId(pid)
                    .txnType(TxnType.BUY)
                    .txnAmt(amount)
                    .quantity(qty)
                    .txnDate(changeDate)
                    .build());

            ObjectNode line = om.createObjectNode();
            line.put("productType", type.name());
            line.put("productId",  pid);
            line.put("amount",     amount.longValue());
            line.put("quantity",   qty.toPlainString());
            buyBreakdown.add(line);
        }

        // 5) CASH 비율만큼 계좌잔액으로 남김
        BigDecimal cashLeft = proceed.multiply(pct(cashRatio)).setScale(0, RoundingMode.DOWN);
        if(accountType==AccountType.IRP){
            IrpAccount a = irpRepo.findByIrpAcctNo(acountId).orElseThrow();
            a.setBalance(nz(a.getBalance()).add(cashLeft));
            irpRepo.save(a);
        }else{
            Long dcId = Long.valueOf(acountId);
            DcAccount a = dcRepo.findById(dcId).orElseThrow();
            a.setBalance(nz(BigDecimal.valueOf(a.getBalance()==null?0:a.getBalance())).add(cashLeft).longValue());
            dcRepo.save(a);
        }

        ObjectNode out = om.createObjectNode();
        out.put("status","APPLIED");
        out.put("changedAt", changeDate.toString());
        out.put("sellAmt", proceed.longValue());
        out.put("interest", interestSum.longValue());
        out.set("buyBreakdown", buyBreakdown);
        out.put("cashLeft", cashLeft.longValue());
        out.set("portfolio", om.valueToTree(
                portfolioRepo.findByAccountTypeAndAcountId(accountType, acountId)));
        return out;
    }

    // ------------------------------------------------------------------------
    // 핸들러에서 요구한 메서드명(호출 시그니처 유지) - 내부에서 실제 구현으로 위임
    // ------------------------------------------------------------------------

    /** [만기예정 목록 조회] (핸들러에서 호출) */
    public JsonNode listMonthlyPrincipalContributions(JsonNode req){
        return list(req);
    }

    /** [만기예정 변경예약 적용] (핸들러에서 호출) */
    public JsonNode applyReservation(JsonNode req){
        return apply(req);
    }
}
