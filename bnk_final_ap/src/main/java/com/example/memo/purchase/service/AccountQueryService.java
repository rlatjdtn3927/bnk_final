// Service (계좌 조회)
package com.example.memo.purchase.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountQueryService {
    private final IrpAccountRepository irpRepo;
    private final DcAccountRepository dcRepo;

    public String getIrpAcctNoByUserId(Long userId) {
        IrpAccount irp = irpRepo.findOneByUserId(userId);
        return irp != null ? irp.getIrpAcctNo() : null;
    }

    public List<String> getDcAccountNosByUserId(Long userId) {
        return dcRepo.findAllByUserId(userId).stream()
                .map(DcAccount::getAccountNo)
                .toList();
    }
}
