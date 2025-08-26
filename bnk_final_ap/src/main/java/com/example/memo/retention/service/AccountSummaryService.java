package com.example.memo.retention.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        // 2. 계좌별 잔액, 손익, 수익률 갱신
        for (AccountSummaryDto account : accounts) {

            // JSON 파라미터 생성
            ObjectNode params = objectMapper.createObjectNode();
            params.put("accountType", account.getAccountType()); // "IRP" or "DC"
            params.put("accountId", account.getAccountNo());

            // holdings 조회
            ResponseAccountHoldingsDto holdings = changeProductService.getHoldings(params);

            // ★ 수익률 계산을 위한 변수 초기화
            BigDecimal totalValuation = BigDecimal.ZERO; // 총 평가금액
            BigDecimal totalPrincipal = BigDecimal.ZERO; // 총 원금 (매입금액)

            // 펀드 합산
            for (FundHoldingsDto fund : holdings.getFundHoldings()) {
                BigDecimal valuation = fund.getValuationAmount() != null ? fund.getValuationAmount() : BigDecimal.ZERO;
                BigDecimal principal = fund.getAcquisitionAmount() != null ? fund.getAcquisitionAmount() : BigDecimal.ZERO;
                
                totalValuation = totalValuation.add(valuation);
                totalPrincipal = totalPrincipal.add(principal); // ★ 펀드 매입 원금 합산
            }

            // 원리금 보장 합산
            for (PrincipalLedgerDto principalLedger : holdings.getPrincipalLedgers()) {
                BigDecimal contract = principalLedger.getContractAmount() != null ? principalLedger.getContractAmount() : BigDecimal.ZERO;
                BigDecimal interest = principalLedger.getInterestAccrued() != null ? principalLedger.getInterestAccrued() : BigDecimal.ZERO;
                
                totalValuation = totalValuation.add(contract).add(interest);
                totalPrincipal = totalPrincipal.add(contract); // ★ 원리금보장 원금 합산
            }

            // ★ 총 손익 및 수익률 계산
            BigDecimal totalProfitLoss = totalValuation.subtract(totalPrincipal);
            BigDecimal returnRate = BigDecimal.ZERO;
            
            // 원금이 0보다 클 때만 수익률 계산 (0으로 나누기 방지)
            if (totalPrincipal.compareTo(BigDecimal.ZERO) > 0) {
                returnRate = totalValuation.divide(totalPrincipal, 4, RoundingMode.HALF_UP) // 소수점 4자리까지 계산
                                        .subtract(BigDecimal.ONE)
                                        .multiply(new BigDecimal("100"));
            }

            // ★ 계산된 최종 값으로 DTO 갱신
            account.setBalance(totalValuation); // 총 평가금액으로 잔액 설정
            account.setTotalProfitLoss(totalProfitLoss); // 총 손익 설정
            account.setReturnRate(returnRate); // 수익률 설정
        }

        // 3. 응답 생성
        return AccountsResponse.builder()
                .userName(accounts.isEmpty() ? "" : accounts.get(0).getUserName())
                .accounts(accounts)
                .build();
    }
}
