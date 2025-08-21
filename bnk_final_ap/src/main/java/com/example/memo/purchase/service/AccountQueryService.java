package com.example.memo.purchase.service;

import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryService {

    private final IrpAccountRepository irpRepo;
    private final DcAccountRepository dcRepo;

    /** IRP 계좌번호 1건 (없으면 null) */
    public String getIrpAcctNoByUserId(Long userId) {
        return irpRepo.findIrpAcctNoByUserId(userId);
    }

    /** DC 계좌번호 N건 (없으면 빈 리스트) */
    public List<String> getDcAccountNosByUserId(Long userId) {
        List<String> list = dcRepo.findAccountNosByUserId(userId);
        return (list == null) ? List.of() : list;
    }
}
