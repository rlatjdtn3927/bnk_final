package com.example.memo.irp.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.example.memo.irp.entity.IrpAccount;
import com.example.memo.irp.entity.IrpJoinEntity;
import com.example.memo.irp.entity.IrpRetirePurpose;
import com.example.memo.irp.entity.IrpTaxPurpose;
import com.example.memo.irp.entity.IrpTransferPurpose;
import com.example.memo.irp.repository.BankAccountRepository;
import com.example.memo.irp.repository.IrpAccountRepository;
import com.example.memo.irp.repository.IrpJoinRepository;
import com.example.memo.irp.repository.IrpRetireRepository;
import com.example.memo.irp.repository.IrpTaxRepository;
import com.example.memo.irp.repository.IrpTransferRepository;
import com.example.memo.irp.util.AccountNumberGenerator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrpJoinService {
	
	private final IrpJoinRepository joinRepository;
	private final IrpTaxRepository taxRepository;
	private final IrpRetireRepository retireRepository;
	private final IrpTransferRepository transferRepository;
	private final IrpAccountRepository irpAccountRepository;
    private final AccountNumberGenerator accountNumberGenerator; // 커스텀 유틸
    private final BankAccountRepository bankRepository;
	
	@Transactional
	public IrpJoinEntity saveJoin(IrpJoinEntity irpJoinEntity) {
		return joinRepository.save(irpJoinEntity);
	}
	
	//가입목적-세액공제
	@Transactional
	public void saveTaxPurpose(Long joinId, String qualType, String busiNo) {
		IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow();
		IrpTaxPurpose tax = IrpTaxPurpose.builder()
				.join(join)
				.irpQualType(qualType)
				.businessNo(busiNo)
				.build();
		taxRepository.save(tax);
	}
	
	//가입목적-퇴직금 수령
	@Transactional
	public void saveRetirePurpose(Long joinId, LocalDate retireDate, String retireReason, String corpName, Long severAmt, String withholdDoc) {
		IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow();
		IrpRetirePurpose retire = IrpRetirePurpose.builder()
				.join(join)
				.retireDate(retireDate)
				.retireReason(retireReason)
				.corpName(corpName)
				.severanceAmt(severAmt)
				.withholdDoc(withholdDoc)
				.build();
		retireRepository.save(retire);
	}
	
	//가입목적-계약이전
	@Transactional
	public void saveTransferPurpose(Long joinId, String prevBank, String prevAccount, Long transferAmt, String transferDoc) {
		IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow();
		IrpTransferPurpose transfer = IrpTransferPurpose.builder()
				.join(join)
				.prevBank(prevBank)
				.prevAccount(prevAccount)
				.transferAmt(transferAmt)
				.transferDoc(transferDoc)
				.build();
		transferRepository.save(transfer);
	}
	
	//가입성공 후 계좌개설
	@Transactional
	public String completeJoinAndOpenIrpAccount(Long joinId, String irpPwd) {
		
		// 1) 가입건 조회
		IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow();
		
		//2) 계좌번호 생성
        String newAcctNo = accountNumberGenerator.next(); //예: "IRP-20250808-000123"
        
        //3) IRP 계좌 생성
        IrpAccount acct = IrpAccount.builder()
                .irpAcctNo(newAcctNo)
                .user(join.getUserId())
                .balance(BigDecimal.ZERO)
                .contractNo(join.getContractNo())
                .irpPwd(irpPwd)
                .status("ACTIVE")
                .build();
        irpAccountRepository.save(acct);
        
        // 4) 가입건에 계좌 연결
        join.setIrpAcctNo(acct);
        joinRepository.save(join);
		
		return newAcctNo;
	}
	
}
