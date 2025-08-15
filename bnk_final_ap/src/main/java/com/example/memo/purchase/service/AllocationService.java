// src/main/java/com/example/memo/purchase/service/AllocationService.java
package com.example.memo.purchase.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate; // 🔸 LocalDate 사용
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.common.ProductType;
import com.example.memo.jpa.entity.purchase.common.TxnType;
import com.example.memo.jpa.entity.purchase.portfolio.Portfolio;
import com.example.memo.jpa.entity.purchase.portfolio.TransactionHistory;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundNavRepository;
import com.example.memo.jpa.repository.purchase.portfolio.PortfolioRepository;
import com.example.memo.jpa.repository.purchase.portfolio.TransactionHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AllocationService {

    private final ObjectMapper om;
    private final IrpAccountRepository irpRepo;
    private final DcAccountRepository dcRepo;
    private final PortfolioRepository portfolioRepo;
    private final TransactionHistoryRepository txnRepo;
    private final FundNavRepository fundNavRepo;

    private static BigDecimal nz(BigDecimal v){ return v==null? BigDecimal.ZERO : v; }
    private AccountType parseAccountType(String v){ return v==null? null : AccountType.valueOf(v.trim().toUpperCase()); }
    private ProductType parseProductTypeOrNull(String v){
        if(v==null) return null;
        String x = v.trim().toUpperCase();
        if("CASH".equals(x)) return null; // CASH는 상품 아님(잔액)
        return ProductType.valueOf(x);
    }

    /** 미리보기 */
    public JsonNode preview(JsonNode req){
        ObjectNode out = om.createObjectNode();
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText(); // String

        int sum=0; for(JsonNode it : req.withArray("items")) sum+= it.path("ratio").asInt();
        if(sum!=100){ out.put("error","운용비율 합계 100% 필요"); return out; }

        BigDecimal cash;
        if(accountType == AccountType.IRP){
            cash = irpRepo.findByIrpAcctNo(acountId).map(IrpAccount::getBalance).orElse(BigDecimal.ZERO);
        }else{
            Long dcId = Long.valueOf(acountId);
            cash = dcRepo.findById(dcId).map(a-> BigDecimal.valueOf(a.getBalance()==null?0L:a.getBalance()))
                    .orElse(BigDecimal.ZERO);
        }

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

    /** 적용(즉시) */
    @Transactional
    public JsonNode apply(JsonNode req){
        ObjectNode out = om.createObjectNode();
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText(); // String
        String acctPwd          = req.path("acctPwd").asText("");
        LocalDate today         = LocalDate.now(); // 🔸 엔티티의 날짜 타입(LocalDate)에 맞춤

        // IRP 비밀번호 체크(샘플)
        if(accountType == AccountType.IRP){
            IrpAccount acct = irpRepo.findByIrpAcctNo(acountId)
                    .orElseThrow(() -> new IllegalArgumentException("IRP 계좌 없음"));
            if(!Objects.equals(acct.getIrpPwd(), acctPwd)){
                return om.createObjectNode().put("error","비밀번호 불일치");
            }
        }

        // 비율 합계 100%
        int sum=0; for(JsonNode it : req.withArray("items")) sum+= it.path("ratio").asInt();
        if(sum!=100){ return om.createObjectNode().put("error","운용비율 합계 100% 필요"); }

        // 사용 가능 현금
        BigDecimal cash;
        if(accountType == AccountType.IRP){
            cash = irpRepo.findByIrpAcctNo(acountId).map(IrpAccount::getBalance).orElse(BigDecimal.ZERO);
        }else{
            Long dcId = Long.valueOf(acountId);
            cash = dcRepo.findById(dcId).map(a-> BigDecimal.valueOf(a.getBalance()==null?0L:a.getBalance()))
                    .orElse(BigDecimal.ZERO);
        }

        // BUY 집행(CASH 제외)
        for(JsonNode it : req.withArray("items")){
            String typeStr = it.path("productType").asText();
            ProductType type = parseProductTypeOrNull(typeStr);
            String pid  = it.path("productId").asText(null);
            int ratio   = it.path("ratio").asInt();

            BigDecimal amount = cash.multiply(BigDecimal.valueOf(ratio))
                                    .divide(BigDecimal.valueOf(100),0, RoundingMode.DOWN);
            if(type == null) continue; // CASH는 남김

            BigDecimal qty;
            if(type == ProductType.FUND || type == ProductType.ETF || type == ProductType.TDF){
                BigDecimal nav = fundNavRepo.findTopByFund_ProductIdOrderByReferenceDateDesc(pid)
                        .map(n -> nz(n.getNav())).filter(n->n.signum()>0).orElse(BigDecimal.ONE);
                qty = amount.divide(nav, 2, RoundingMode.DOWN);
            }else{
                qty = amount; // PRINCIPAL: 1:1
            }

            // upsert 포트폴리오 (acountId: String)
            Portfolio p = portfolioRepo
                    .findByAccountTypeAndAcountIdAndProductTypeAndProductId(accountType, acountId, type, pid)
                    .orElseGet(() -> Portfolio.builder()
                            .accountType(accountType)
                            .acountId(acountId) // String
                            .productType(type)
                            .productId(pid)
                            .quantity(BigDecimal.ZERO)
                            .investAmt(BigDecimal.ZERO)
                            .evalAmt(BigDecimal.ZERO)
                            .startDate(today) // 🔸 LocalDate
                            .build());

            p.setQuantity(nz(p.getQuantity()).add(qty));
            p.setInvestAmt(nz(p.getInvestAmt()).add(amount));
            p.setEvalAmt(nz(p.getEvalAmt()).add(amount));
            portfolioRepo.save(p);

            // 거래내역 기록 (txnDate: LocalDate)
            txnRepo.save(TransactionHistory.builder()
                    .accountType(accountType)
                    .acountId(acountId)  // String
                    .productId(pid)
                    .txnType(TxnType.BUY)
                    .txnAmt(amount)
                    .quantity(qty)
                    .txnDate(today)      // 🔸 LocalDate
                    .build());
        }

        // 잔액 = CASH 비율만큼 유지
        int cashRatio=0;
        for(JsonNode it : req.withArray("items")){
            if("CASH".equalsIgnoreCase(it.path("productType").asText())) {
                cashRatio = it.path("ratio").asInt();
                break;
            }
        }
        BigDecimal targetCash = cash.multiply(BigDecimal.valueOf(cashRatio))
                                    .divide(BigDecimal.valueOf(100),0, RoundingMode.DOWN);

        if(accountType == AccountType.IRP){
            IrpAccount a = irpRepo.findByIrpAcctNo(acountId).orElseThrow();
            a.setBalance(targetCash); irpRepo.save(a);
        }else{
            Long dcId = Long.valueOf(acountId);
            DcAccount a = dcRepo.findById(dcId).orElseThrow();
            a.setBalance(targetCash.longValue()); dcRepo.save(a);
        }

        out.put("status","APPLIED");
        out.put("appliedAt", today.toString());
        out.put("accountType", accountType.name());
        out.put("acountId", acountId);
        out.set("portfolio", om.valueToTree(
                portfolioRepo.findByAccountTypeAndAcountId(accountType, acountId)));
        return out;
    }
}
