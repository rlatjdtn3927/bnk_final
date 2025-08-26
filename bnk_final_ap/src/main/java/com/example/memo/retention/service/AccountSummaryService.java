package com.example.memo.retention.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.memo.purchase.change.dto.response.FundHoldingsDto;
import com.example.memo.purchase.change.dto.response.PrincipalLedgerDto;
import com.example.memo.purchase.change.dto.response.ResponseAccountHoldingsDto;
import com.example.memo.purchase.change.service.ChangeProductService;
import com.example.memo.retention.dto.AccountSummaryDto;
import com.example.memo.retention.dto.AccountsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountSummaryService {

    private final RetainAccountService retainAccountService;
    private final ChangeProductService changeProductService;
    private final ObjectMapper objectMapper;

    public AccountsResponse getAccounts(Long userId) {

        // 1. 사용자 계좌 목록 조회
        List<AccountSummaryDto> accounts = retainAccountService.findAccountsOfUser(userId);

        // 2. 계좌별 잔액 갱신
        for (AccountSummaryDto account : accounts) {

            // JSON 파라미터 생성
            ObjectNode params = objectMapper.createObjectNode();
            params.put("accountType", account.getAccountType()); // "IRP" or "DC"
            params.put("accountId", account.getAccountNo());

            // holdings 조회
            ResponseAccountHoldingsDto holdings = changeProductService.getHoldings(params);

            BigDecimal balance = BigDecimal.ZERO;

            // 펀드 합산
            for (FundHoldingsDto fund : holdings.getFundHoldings()) {
                balance = balance.add(
                        fund.getValuationAmount() != null
                                ? fund.getValuationAmount()
                                : BigDecimal.ZERO
                );
            }

            // 원리금 보장 합산
            for (PrincipalLedgerDto principal : holdings.getPrincipalLedgers()) {
                BigDecimal contract = principal.getContractAmount() != null
                        ? principal.getContractAmount()
                        : BigDecimal.ZERO;
                BigDecimal interest = principal.getInterestAccrued() != null
                        ? principal.getInterestAccrued()
                        : BigDecimal.ZERO;
                balance = balance.add(contract.add(interest));
            }

            // balance 갱신
            account.setBalance(balance);
        }

        // 3. 응답 생성
        return AccountsResponse.builder()
                .userName(accounts.isEmpty() ? "" : accounts.get(0).getUserName())
                .accounts(accounts)
                .build();
    }
}
