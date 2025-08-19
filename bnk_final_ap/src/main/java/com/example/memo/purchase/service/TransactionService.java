// src/main/java/com/example/memo/purchase/service/TransactionService.java
package com.example.memo.purchase.service;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.repository.purchase.portfolio.TransactionHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * FLOW: 거래내역(TRANSACTION_HISTORY) 조회
 * - 입력: accountType, acountId
 * - 출력: 해당 계좌의 전체 거래내역을 최신순으로 반환
 * - 화면/다른 서비스에서 추가 가공해서 사용
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final ObjectMapper om;
    private final TransactionHistoryRepository txnRepo;

    private AccountType parseAccountType(String v){
        return v==null? null : AccountType.valueOf(v.trim().toUpperCase());
    }

    /** FLOW: 계좌 전체 거래내역 최신순 */
    public JsonNode getTransactions(JsonNode req){
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText(); // String (IRP계좌번호 or DC ID)

        return om.valueToTree(
                txnRepo.findByAccountTypeAndAcountIdOrderByTxnDateDesc(accountType, acountId));
    }
}
