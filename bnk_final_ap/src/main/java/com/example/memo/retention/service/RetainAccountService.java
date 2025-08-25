package com.example.memo.retention.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.retention.dto.AccountSummaryDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RetainAccountService {

	private final IrpAccountRepository irpRepo;
    private final DcAccountRepository dcRepo;

    @Transactional(readOnly=true)
    public List<AccountSummaryDto> findAccountsOfUser(Long userId) {
        List<AccountSummaryDto> out = new ArrayList<>();

        // IRP 계좌 (하나만)
        irpRepo.findByUser_UserId(userId).ifPresent(a -> {
            out.add(AccountSummaryDto.builder()
                .accountType("IRP")
                .accountNo(a.getIrpAcctNo())
                .displayName("개인IRP")
                .balance(a.getBalance() == null ? BigDecimal.ZERO : a.getBalance())
                .userName(a.getUser().getName())
                .build());
        });

        // DC 계좌 (하나만)
        dcRepo.findByDcMember_User_UserId(userId)
              .stream()
              .findFirst()
              .ifPresent(a -> {
                  out.add(AccountSummaryDto.builder()
                      .accountType("DC")
                      .accountNo(a.getAccountNo())
                      .displayName("퇴직연금 DC")
                      .balance(a.getBalance() == null ? BigDecimal.ZERO : a.getBalance())
                      .userName(a.getDcMember().getUser().getName())
                      .build());
              });

        return out;
    }
}

