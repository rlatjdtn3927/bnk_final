// src/main/java/com/example/memo/purchase/service/TransactionService.java
package com.example.memo.purchase.service;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.repository.purchase.portfolio.TransactionHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final ObjectMapper om;
    private final TransactionHistoryRepository txnRepo;

    private AccountType parseAccountType(String v){
        return v==null? null : AccountType.valueOf(v.trim().toUpperCase());
    }

    public JsonNode getTransactions(JsonNode req){
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText(); // 🔸 String
        return om.valueToTree(
                txnRepo.findByAccountTypeAndAcountIdOrderByTxnDateDesc(accountType, acountId));
    }
}
