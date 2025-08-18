// src/main/java/com/example/memo/purchase/service/SummaryService.java
package com.example.memo.purchase.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.common.ProductType;
import com.example.memo.jpa.entity.purchase.portfolio.Portfolio;
import com.example.memo.jpa.repository.purchase.analysis.FundNavRepository;
import com.example.memo.jpa.repository.purchase.portfolio.DepositHistoryRepository;
import com.example.memo.jpa.repository.purchase.portfolio.PortfolioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final ObjectMapper om;
    private final PortfolioRepository portfolioRepo;
    private final FundNavRepository fundNavRepo;
    private final DepositHistoryRepository depositRepo;

    private static BigDecimal nz(BigDecimal v){ return v==null? BigDecimal.ZERO : v; }
    private AccountType parseAccountType(String v){ return v==null? null : AccountType.valueOf(v.trim().toUpperCase()); }

    public JsonNode getSummary(JsonNode req){
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText(); // 🔸 String
        ObjectNode out          = om.createObjectNode();

        List<Portfolio> list = portfolioRepo.findByAccountTypeAndAcountId(accountType, acountId);
        BigDecimal totalEval  = BigDecimal.ZERO;

        for(Portfolio p : list){
            ProductType type = p.getProductType();
            BigDecimal eval;

            if(type == ProductType.FUND || type == ProductType.ETF || type == ProductType.TDF){
                BigDecimal qty = nz(p.getQuantity());
                BigDecimal nav = fundNavRepo.findTopByFund_ProductIdOrderByReferenceDateDesc(p.getProductId())
                        .map(n-> n.getNav()==null? BigDecimal.ONE : n.getNav())
                        .orElse(BigDecimal.ONE);
                eval = qty.multiply(nav);
            }else if(type == ProductType.PRINCIPAL){
                eval = p.getEvalAmt()!=null ? p.getEvalAmt() : nz(p.getQuantity());
            }else{ // (CASH 등) enum 외 타입이 들어올 일은 없지만 방어
                eval = nz(p.getEvalAmt());
            }
            totalEval = totalEval.add(eval);
        }

        BigDecimal totalDeposit = depositRepo
                .findByAccountTypeAndAcountIdOrderByDepositDateDesc(accountType, acountId)
                .stream().map(d-> nz(d.getDepositAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        BigDecimal todayDeposit = depositRepo
                .findByAccountTypeAndAcountIdAndDepositDateBetween(
                        accountType, acountId, today.atStartOfDay(), today.plusDays(1).atStartOfDay())
                .stream().map(d-> nz(d.getDepositAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);

        out.put("totalEvalAmt", totalEval.longValue());
        out.put("totalProfitRate", 0);
        out.put("totalDepositAmt", totalDeposit.longValue());
        out.put("todayDepositAmt", todayDeposit.longValue());
        out.put("opProfitAmt", 0);
        out.put("accountType", accountType==null? "" : accountType.name());
        out.put("acountId", acountId);
        return out;
    }
}
