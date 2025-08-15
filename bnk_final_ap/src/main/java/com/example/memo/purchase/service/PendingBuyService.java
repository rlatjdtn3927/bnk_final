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

    public JsonNode history(JsonNode req){
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText(); // 🔸 String

        ArrayNode out = om.createArrayNode();
        txnRepo.findByAccountTypeAndAcountIdOrderByTxnDateDesc(accountType, acountId).forEach(tx -> {
            String pid  = tx.getProductId();
            String name = fundRepo.findByProductId(pid).map(f->f.getProductName())
                    .orElseGet(() -> principalRepo.findByProductId(pid).map(p->p.getProductName()).orElse("-"));

            BigDecimal nav = fundNavRepo.findTopByFund_ProductIdOrderByReferenceDateDesc(pid)
                    .map(n-> n.getNav()==null? BigDecimal.ONE : n.getNav())
                    .orElse(BigDecimal.ONE);
            BigDecimal qty  = tx.getQuantity()==null? BigDecimal.ZERO : tx.getQuantity();
            BigDecimal eval = qty.multiply(nav);

            String dateStr;
            Object raw = tx.getTxnDate();
            if (raw instanceof LocalDateTime dt)      dateStr = dt.toLocalDate().toString();
            else if (raw instanceof LocalDate d)      dateStr = d.toString();
            else if (raw != null)                     dateStr = raw.toString();
            else                                      dateStr = "-";

            out.add(om.createObjectNode()
                    .put("txnDate",     dateStr)
                    .put("accountType", tx.getAccountType()==null? "" : tx.getAccountType().name())
                    .put("productId",   pid)
                    .put("productName", name)
                    .put("evalAmt",     eval.longValue()));
        });
        return out;
    }

    public JsonNode list(JsonNode req){ return om.createArrayNode(); }
    public JsonNode upsert(JsonNode req){ return om.createObjectNode().put("status","OK"); }
}
