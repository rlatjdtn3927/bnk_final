// src/main/java/com/example/memo/purchase/service/AllocationService.java
package com.example.memo.purchase.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.irp.IrpAccount;
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
 * FLOW: 운용비율 배분(등록/변경/만기)
 * - PENDING/CHANGE: 기존 로직 유지
 * - MATURITY: 예금 전환(납입건별 일할이자 -> 전량 SELL -> 새 비율 BUY -> CASH 잔액 반영)
 */
@Service
@RequiredArgsConstructor
public class AllocationService {

    private final ObjectMapper om;
    private final IrpAccountRepository irpRepo;
    private final DcAccountRepository dcRepo;
    private final PortfolioRepository portfolioRepo;
    private final TransactionHistoryRepository txnRepo;
    private final FundNavRepository fundNavRepo;
    private final PrincipalGuaranteeRepository principalRepo;

    private static BigDecimal nz(BigDecimal v){ return v==null? BigDecimal.ZERO : v; }
    private AccountType parseAccountType(String v){ return v==null? null : AccountType.valueOf(v.trim().toUpperCase()); }
    private ProductType parseProductTypeOrNull(String v){
        if(v==null) return null;
        String x = v.trim().toUpperCase();
        if("CASH".equals(x)) return null; // CASH는 상품 아님(잔액)
        return ProductType.valueOf(x);
    }

    // ─────────────────────────────────────
    // FLOW: 미리보기 (PENDING/CHANGE/MATURITY)
    // ─────────────────────────────────────
    public JsonNode preview(JsonNode req){
        String flow = req.path("flow").asText("PENDING").trim().toUpperCase();
        if ("MATURITY".equals(flow)) return previewMaturity(req);
        return previewStandard(req);
    }

    // 일반(PENDING/CHANGE)
    private JsonNode previewStandard(JsonNode req){
        ObjectNode out = om.createObjectNode();
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText();

        int sum=0; for(JsonNode it : req.withArray("items")) sum+= it.path("ratio").asInt();
        if(sum!=100){ out.put("error","운용비율 합계 100% 필요"); return out; }

        BigDecimal cash = getBalance(accountType, acountId);

        ArrayNode after = om.createArrayNode();
        for(JsonNode it : req.withArray("items")){
            String typeStr = it.path("productType").asText();
            ProductType type = parseProductTypeOrNull(typeStr);
            String pid  = it.path("productId").asText(null);
            int ratio   = it.path("ratio").asInt();

            ObjectNode line = om.createObjectNode();
            line.put("productType", typeStr);
            line.put("productId",   pid);
            line.put("ratio",       ratio);

            BigDecimal amount = cash.multiply(BigDecimal.valueOf(ratio))
                                    .divide(BigDecimal.valueOf(100),0, RoundingMode.DOWN);

            if(type == null){ // CASH
                line.put("estAmount", amount.longValue());
                line.put("estQuantity", 0);
            }else if(type == ProductType.FUND || type == ProductType.ETF || type == ProductType.TDF){
                BigDecimal nav = fundNavRepo.findTopByFund_ProductIdOrderByReferenceDateDesc(pid)
                        .map(n -> nz(n.getNav())).filter(n->n.signum()>0).orElse(BigDecimal.ONE);
                BigDecimal qty = amount.divide(nav, 2, RoundingMode.DOWN);
                line.put("estAmount", amount.longValue());
                line.put("estQuantity", qty.toPlainString());
            }else{ // PRINCIPAL
                line.put("estAmount", amount.longValue());
                line.put("estQuantity", amount.longValue());
            }
            after.add(line);
        }

        out.set("before", om.valueToTree(
                portfolioRepo.findByAccountTypeAndAcountId(accountType, acountId)));
        out.set("after", after);
        out.put("accountType", accountType.name());
        out.put("acountId", acountId);
        return out;
    }

    // 만기(MATURITY)
    private JsonNode previewMaturity(JsonNode req){
        ObjectNode out = om.createObjectNode();
        AccountType at = parseAccountType(req.path("accountType").asText());
        String acId    = req.path("acountId").asText();

        int sum=0; for(JsonNode it : req.withArray("items")) sum+= it.path("ratio").asInt();
        if(sum!=100){ return om.createObjectNode().put("error","운용비율 합계 100% 필요"); }

        String sourcePid    = req.path("maturity").path("sourcePid").asText();
        LocalDate changeDate= parseDate(req.path("maturity").path("changeDate").asText(), LocalDate.now());

        // ① 소스 포지션 점검
        Portfolio src = portfolioRepo
                .findByAccountTypeAndAcountIdAndProductTypeAndProductId(at, acId, ProductType.PRINCIPAL, sourcePid)
                .orElse(null);
        if (src == null) return om.createObjectNode().put("error","전환 대상 예금 포지션 없음");

        // ② 금리(계좌유형별)
        BigDecimal rate = resolvePrincipalRate(sourcePid, at); // IRP/DC/DB 구분 금리

        // ③ 납입건별(BUY) 일할이자 합계
        List<TransactionHistory> buys = txnRepo
                .findByAccountTypeAndAcountIdAndTxnTypeAndProductIdAndTxnDateLessThanEqual(
                        at, acId, TxnType.BUY, sourcePid, changeDate);

        BigDecimal principal = BigDecimal.ZERO;
        BigDecimal interest  = BigDecimal.ZERO;
        for (TransactionHistory t : buys){
            BigDecimal amt = nz(t.getTxnAmt());
            long days = Math.max(0, ChronoUnit.DAYS.between(t.getTxnDate(), changeDate));
            BigDecimal itz = amt.multiply(rate)
                                .multiply(BigDecimal.valueOf(days))
                                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
            principal = principal.add(amt);
            interest  = interest.add(itz);
        }
        BigDecimal available = principal.add(interest);

        ObjectNode after = om.createObjectNode();
        after.put("sourceClosed", sourcePid);
        after.put("principal", principal.toPlainString());
        after.put("interest",  interest.toPlainString());
        after.put("available", available.toPlainString());
        after.put("annualRate", rate.toPlainString());

        ArrayNode target = om.createArrayNode();
        for(JsonNode it : req.withArray("items")){
            target.add(om.createObjectNode()
                    .put("productType", it.path("productType").asText())
                    .put("productId",   it.path("productId").asText(""))
                    .put("ratio",       it.path("ratio").asInt()));
        }
        after.set("target", target);

        out.set("before", om.valueToTree(
                portfolioRepo.findByAccountTypeAndAcountId(at, acId)));
        out.set("after", after);
        out.put("ok", true);
        return out;
    }

    // ─────────────────────────────────────
    // FLOW: 적용 (PENDING/CHANGE/MATURITY)
    // ─────────────────────────────────────
    @Transactional
    public JsonNode apply(JsonNode req){
        String flow = req.path("flow").asText("PENDING").trim().toUpperCase();
        if ("MATURITY".equals(flow)) return applyMaturity(req);
        return applyStandard(req);
    }

    // 일반(PENDING/CHANGE) – 기존 그대로
    @Transactional
    public JsonNode applyStandard(JsonNode req){
        ObjectNode out = om.createObjectNode();
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText();
        String acctPwd          = req.path("acctPwd").asText("");
        LocalDate today         = LocalDate.now();

        // FLOW: 계좌비번 검증(요청사항: DB 문자열 일치만)
        if(accountType == AccountType.IRP){
            IrpAccount acct = irpRepo.findByIrpAcctNo(acountId)
                    .orElseThrow(() -> new IllegalArgumentException("IRP 계좌 없음"));
            if(!Objects.equals(acct.getIrpPwd(), acctPwd)){
                return om.createObjectNode().put("error","비밀번호 불일치");
            }
        }

        // FLOW: 비율 합계 100%
        int sum=0; for(JsonNode it : req.withArray("items")) sum+= it.path("ratio").asInt();
        if(sum!=100){ return om.createObjectNode().put("error","운용비율 합계 100% 필요"); }

        // FLOW: 사용 가능 현금
        BigDecimal cash = getBalance(accountType, acountId);

        // FLOW: 상품 BUY (CASH 제외)
        for(JsonNode it : req.withArray("items")){
            String typeStr = it.path("productType").asText();
            ProductType type = parseProductTypeOrNull(typeStr);
            String pid  = it.path("productId").asText(null);
            int ratio   = it.path("ratio").asInt();

            BigDecimal amount = cash.multiply(BigDecimal.valueOf(ratio))
                                    .divide(BigDecimal.valueOf(100),0, RoundingMode.DOWN);
            if(type == null) continue; // CASH는 잔액으로 유지

            BigDecimal qty;
            if(type == ProductType.FUND || type == ProductType.ETF || type == ProductType.TDF){
                BigDecimal nav = fundNavRepo.findTopByFund_ProductIdOrderByReferenceDateDesc(pid)
                        .map(n -> nz(n.getNav())).filter(n->n.signum()>0).orElse(BigDecimal.ONE);
                qty = amount.divide(nav, 2, RoundingMode.DOWN);
            }else{ // PRINCIPAL
                qty = amount; // 1:1
            }

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
                            .startDate(today)
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
                    .txnDate(today)
                    .build());
        }

        // FLOW: 잔액 = CASH 비율만큼 유지
        int cashRatio=0;
        for(JsonNode it : req.withArray("items")){
            if("CASH".equalsIgnoreCase(it.path("productType").asText())) {
                cashRatio = it.path("ratio").asInt();
                break;
            }
        }
        BigDecimal targetCash = cash.multiply(BigDecimal.valueOf(cashRatio))
                                    .divide(BigDecimal.valueOf(100),0, RoundingMode.DOWN);
        setBalance(accountType, acountId, targetCash);

        out.put("status","APPLIED");
        out.put("appliedAt", today.toString());
        out.put("accountType", accountType.name());
        out.put("acountId", acountId);
        out.set("portfolio", om.valueToTree(
                portfolioRepo.findByAccountTypeAndAcountId(accountType, acountId)));
        return out;
    }

    // 만기(MATURITY) – 납입건별 일할이자 + 전량 SELL + 새 비율 BUY
    @Transactional
    private JsonNode applyMaturity(JsonNode req){
        ObjectNode out = om.createObjectNode();
        AccountType at = parseAccountType(req.path("accountType").asText());
        String acId    = req.path("acountId").asText();
        String acctPwd = req.path("acctPwd").asText("");
        LocalDate today= LocalDate.now();

        // FLOW: 비밀번호 검증(문자열 일치)
        if(at == AccountType.IRP){
            IrpAccount acct = irpRepo.findByIrpAcctNo(acId)
                    .orElseThrow(() -> new IllegalArgumentException("IRP 계좌 없음"));
            if(!Objects.equals(acct.getIrpPwd(), acctPwd)){
                return om.createObjectNode().put("error","비밀번호 불일치");
            }
        }

        // FLOW: 비율 합계 100%
        int sum=0; for(JsonNode it : req.withArray("items")) sum+= it.path("ratio").asInt();
        if(sum!=100){ return om.createObjectNode().put("error","운용비율 합계 100% 필요"); }

        String sourcePid    = req.path("maturity").path("sourcePid").asText();
        LocalDate changeDate= parseDate(req.path("maturity").path("changeDate").asText(), LocalDate.now());

        // ① 소스 포지션 존재 확인
        Portfolio src = portfolioRepo
                .findByAccountTypeAndAcountIdAndProductTypeAndProductId(at, acId, ProductType.PRINCIPAL, sourcePid)
                .orElse(null);
        if (src == null) return om.createObjectNode().put("error","전환 대상 예금 포지션 없음");

        // ② 계좌유형별 금리
        BigDecimal rate = resolvePrincipalRate(sourcePid, at);

        // ③ 납입건별(BUY) 일할이자 합산
        List<TransactionHistory> buys = txnRepo
                .findByAccountTypeAndAcountIdAndTxnTypeAndProductIdAndTxnDateLessThanEqual(
                        at, acId, TxnType.BUY, sourcePid, changeDate);

        BigDecimal principal = BigDecimal.ZERO;
        BigDecimal interest  = BigDecimal.ZERO;
        for (TransactionHistory t : buys){
            BigDecimal amt = nz(t.getTxnAmt());
            long days = Math.max(0, ChronoUnit.DAYS.between(t.getTxnDate(), changeDate));
            BigDecimal itz = amt.multiply(rate)
                                .multiply(BigDecimal.valueOf(days))
                                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
            principal = principal.add(amt);
            interest  = interest.add(itz);
        }
        BigDecimal available = principal.add(interest);

        // ④ 소스 포지션 전량 청산(SELL)
        src.setQuantity(BigDecimal.ZERO);
        src.setInvestAmt(BigDecimal.ZERO);
        src.setEvalAmt(BigDecimal.ZERO);
        portfolioRepo.save(src);

        txnRepo.save(TransactionHistory.builder()
                .accountType(at)
                .acountId(acId)
                .productId(sourcePid)
                .txnType(TxnType.SELL)
                .txnAmt(available)        // 원금+이자
                .quantity(BigDecimal.ONE) // 의미상 1
                .txnDate(today)
                .build());

        // ⑤ target 비율대로 BUY (CASH 제외)
        BigDecimal remaining = available;
        int cashRatio = 0;

        for(JsonNode it : req.withArray("items")){
            String typeStr = it.path("productType").asText();
            String pid     = it.path("productId").asText(null);
            int ratio      = it.path("ratio").asInt();

            if("CASH".equalsIgnoreCase(typeStr)){ cashRatio = ratio; continue; }
            ProductType type = parseProductTypeOrNull(typeStr);
            if(type == null || ratio <= 0) continue;

            BigDecimal amount = available.multiply(BigDecimal.valueOf(ratio))
                                         .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            remaining = remaining.subtract(amount);

            BigDecimal qty;
            if(type == ProductType.FUND || type == ProductType.ETF || type == ProductType.TDF){
                BigDecimal nav = fundNavRepo.findTopByFund_ProductIdOrderByReferenceDateDesc(pid)
                        .map(n -> nz(n.getNav())).filter(n->n.signum()>0).orElse(BigDecimal.ONE);
                qty = amount.divide(nav, 2, RoundingMode.DOWN);
            }else{ // PRINCIPAL 재가입
                qty = amount;
            }

            Portfolio p = portfolioRepo
                    .findByAccountTypeAndAcountIdAndProductTypeAndProductId(at, acId, type, pid)
                    .orElseGet(() -> Portfolio.builder()
                            .accountType(at)
                            .acountId(acId)
                            .productType(type)
                            .productId(pid)
                            .quantity(BigDecimal.ZERO)
                            .investAmt(BigDecimal.ZERO)
                            .evalAmt(BigDecimal.ZERO)
                            .startDate(today)
                            .build());

            p.setQuantity(nz(p.getQuantity()).add(qty));
            p.setInvestAmt(nz(p.getInvestAmt()).add(amount));
            p.setEvalAmt(nz(p.getEvalAmt()).add(amount));
            if (type == ProductType.PRINCIPAL) {
                p.setStartDate(today); // 재가입 시작일
            }
            portfolioRepo.save(p);

            txnRepo.save(TransactionHistory.builder()
                    .accountType(at)
                    .acountId(acId)
                    .productId(pid)
                    .txnType(TxnType.BUY)
                    .txnAmt(amount)
                    .quantity(qty)
                    .txnDate(today)
                    .build());
        }

        // ⑥ CASH 비중 + 라운딩 잔액 → 계좌 잔액으로 반영
        BigDecimal cashAmt = available.multiply(BigDecimal.valueOf(cashRatio))
                                      .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        BigDecimal toCash = cashAmt.add(remaining.max(BigDecimal.ZERO));
        addBalance(at, acId, toCash);

        ObjectNode res = om.createObjectNode();
        res.put("ok", true);
        res.put("msg", "만기(중도) 전환 적용 완료");
        res.put("principal", principal.toPlainString());
        res.put("interest",  interest.toPlainString());
        res.put("available", available.toPlainString());
        res.put("annualRate", rate.toPlainString());
        return res;
    }

    // ────────────── 유틸 ──────────────
    private BigDecimal getBalance(AccountType accountType, String acountId){
        if(accountType == AccountType.IRP){
            return irpRepo.findByIrpAcctNo(acountId).map(IrpAccount::getBalance).orElse(BigDecimal.ZERO);
        }else{
            return dcRepo.findById(Long.valueOf(acountId))
                    .map(a -> BigDecimal.valueOf(a.getBalance()==null?0L:a.getBalance()))
                    .orElse(BigDecimal.ZERO);
        }
    }
    private void setBalance(AccountType accountType, String acountId, BigDecimal target){
        if(accountType == AccountType.IRP){
            irpRepo.findByIrpAcctNo(acountId).ifPresent(a->{ a.setBalance(target); irpRepo.save(a); });
        }else{
            dcRepo.findById(Long.valueOf(acountId)).ifPresent(a->{ a.setBalance(target.longValue()); dcRepo.save(a); });
        }
    }
    private void addBalance(AccountType accountType, String acountId, BigDecimal delta){
        if(delta==null || delta.signum()==0) return;
        if(accountType == AccountType.IRP){
            irpRepo.findByIrpAcctNo(acountId).ifPresent(a->{ a.setBalance(nz(a.getBalance()).add(delta)); irpRepo.save(a); });
        }else{
            dcRepo.findById(Long.valueOf(acountId)).ifPresent(a->{
                long cur = a.getBalance()==null?0L:a.getBalance();
                a.setBalance(BigDecimal.valueOf(cur).add(delta).longValue());
                dcRepo.save(a);
            });
        }
    }
    private LocalDate parseDate(String s, LocalDate def){ try { return LocalDate.parse(s); } catch(Exception e){ return def; } }

    /** FLOW: 계좌유형별 예금 금리 선택(IRP/DC/DB) – 미존재/누락 시 3% 기본 */
    private BigDecimal resolvePrincipalRate(String productId, AccountType at){
        return principalRepo.findById(productId).map(pg -> {
            if (at == AccountType.IRP && pg.getIrpRate()!=null) return pg.getIrpRate();
            if (at == AccountType.DC  && pg.getDcRate()!=null)  return pg.getDcRate();
            if (pg.getDbRate()!=null) return pg.getDbRate();
            return new BigDecimal("0.03");
        }).orElse(new BigDecimal("0.03"));
    }
}
