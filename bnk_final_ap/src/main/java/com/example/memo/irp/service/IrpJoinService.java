package com.example.memo.irp.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.irp.entity.BankAccount;
import com.example.memo.irp.entity.IrpAccount;
import com.example.memo.irp.entity.IrpJoinEntity;
import com.example.memo.irp.entity.IrpRetirePurpose;
import com.example.memo.irp.entity.IrpTaxPurpose;
import com.example.memo.irp.entity.IrpTransferPurpose;
import com.example.memo.irp.entity.ProductMaster;
import com.example.memo.irp.entity.TestUserEntity;
import com.example.memo.irp.repository.BankAccountRepository;
import com.example.memo.irp.repository.IrpAccountRepository;
import com.example.memo.irp.repository.IrpJoinRepository;
import com.example.memo.irp.repository.IrpRetireRepository;
import com.example.memo.irp.repository.IrpTaxRepository;
import com.example.memo.irp.repository.IrpTransferRepository;
import com.example.memo.irp.repository.ProductMasterRepository;
import com.example.memo.irp.repository.TestUserRepository;
import com.example.memo.irp.util.AccountNumberGenerator;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrpJoinService {
	
	private final IrpJoinRepository joinRepository;
	private final IrpTaxRepository taxRepository;
	private final IrpRetireRepository retireRepository;
	private final IrpAccountRepository irpAccountRepository;
    private final AccountNumberGenerator accountNumberGenerator; // 커스텀 유틸
    private final BankAccountRepository bankRepository;
    private final ProductMasterRepository productRepository;
    private final TestUserRepository userRepository;
    
	
	@Transactional
	public IrpJoinEntity saveJoin(IrpJoinEntity irpJoinEntity) {
		return joinRepository.save(irpJoinEntity);
	}
	
	//step1 : 가입목적 선택(userId, joinPurpose만 저장)
	@Transactional
    public Long createDraft(Long userId, String joinPurpose) {
        TestUserEntity user = userRepository.findById(userId).orElseThrow();
        IrpJoinEntity join = IrpJoinEntity.builder()
                .userId(user)
                .joinPurpose(joinPurpose)
                .build();
        return joinRepository.save(join).getJoinId();
    }
	
	//step3-2: 계약정보 업데이트(부분 저장)
    @Transactional
    public void updateContract(Long joinId, Long annualAmt, Long newAmt,
                               String contractNo, String acctNo, String branchOffice) {
        IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow();
        BankAccount acct = bankRepository.findById(acctNo).orElseThrow();
        join.setAnnualContribAmt(annualAmt);
        join.setNewContribAmt(newAmt);
        join.setContractNo(contractNo);
        join.setAcctNo(acct);
        join.setBranchOffice(branchOffice);
        // save 생략 가능(영속상태)
    }
	
	//가입목적 - 세액공제
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
	
	//가입목적 - 퇴직금 수령
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
	
	//step4: 상품등록(선택)저장
	@Transactional
    public void updateJoinProduct(Long joinId, String productId) {
        IrpJoinEntity join = joinRepository.findById(joinId)
                .orElseThrow(() -> new IllegalArgumentException("가입건 없음: " + joinId));
        ProductMaster product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음: " + productId));
        join.setProductId(product);
        // 영속 상태라 save 생략 가능하지만 명시 저장 권장
        joinRepository.save(join);
    }
	
	//완료: 가입성공 후 계좌개설 + join에 연결
	@Transactional
	public String completeJoinAndOpenIrpAccount(Long joinId, String irpPwd) {
		
		// 1) 가입건 조회
		IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow(
				() -> new IllegalArgumentException("가입건 없음: " + joinId));
		
		//이미 계좌 연결되어 있으면 중복 생성 방지
		if (join.getIrpAccount() != null) {
	        return join.getIrpAccount().getIrpAcctNo();
	    }
		
		//2) 계좌번호 생성
        String newAcctNo = accountNumberGenerator.next(); //예: "20250808-00001" (IRP계좌번호)
        String newContractNo = contractNumberGenerator.next(); // 예: IRP2025081100123 (IRP계약번호)
        
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
        join.setIrpAccount(acct);
        joinRepository.save(join);
		
		return newAcctNo;
	}
	
}
