package com.example.memo.irp.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.irp.dto.AccountContractResult;
import com.example.memo.irp.util.AccountNumberGenerator;
import com.example.memo.irp.util.ContractNumberGenerator;
import com.example.memo.jpa.entity.irp.BankAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.irp.IrpJoinEntity;
import com.example.memo.jpa.entity.irp.IrpRetirePurpose;
import com.example.memo.jpa.entity.irp.IrpTaxPurpose;
import com.example.memo.jpa.entity.irp.ProductMaster;
import com.example.memo.jpa.entity.irp.TestUserEntity;
import com.example.memo.jpa.repository.irp.BankAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.jpa.repository.irp.IrpJoinRepository;
import com.example.memo.jpa.repository.irp.IrpRetireRepository;
import com.example.memo.jpa.repository.irp.IrpTaxRepository;
import com.example.memo.jpa.repository.irp.ProductMasterRepository;
import com.example.memo.jpa.repository.irp.TestUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrpJoinService {

    private final BankAccountService bankAccountService;
    
	private final IrpJoinRepository joinRepository;
	private final IrpTaxRepository taxRepository;
	private final IrpRetireRepository retireRepository;
	private final IrpAccountRepository irpAccountRepository;
    private final BankAccountRepository bankRepository;
    private final ProductMasterRepository productRepository;
    private final TestUserRepository userRepository;
    
    private final AccountNumberGenerator accountNumberGenerator; // 커스텀 유틸
    private final ContractNumberGenerator contractNumberGenerator;
	
    @Transactional
    public IrpJoinEntity findByIdOrThrow(Long joinId) {
		return joinRepository.findById(joinId).orElseThrow();
	}
    
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
                               String acctNo, String acctPwd, String branchOffice) {
        IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow();
        
        //1) 계좌 비번 검증 (계좌 소유자 = join.userId)
        Long userId = join.getUserId().getUserId();
        bankAccountService.verifyAccountPassword(acctNo, userId, acctPwd);
        
        //2) 계좌 엔티티 로드
        BankAccount acct = bankRepository.findById(acctNo).orElseThrow();
        
        //3) 값 검증(선택)
        if (annualAmt != null && annualAmt < 0) throw new IllegalArgumentException("annualContribAmt 음수 불가");
        if (newAmt != null && newAmt < 0) throw new IllegalArgumentException("newContribAmt 음수 불가");
        
        join.setAnnualContribAmt(annualAmt);
        join.setNewContribAmt(newAmt);
        join.setAcctNo(acct);
        join.setBranchOffice(branchOffice);
        // save 생략 가능(영속상태)
        joinRepository.save(join);
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
	public AccountContractResult completeJoinAndOpenIrpAccount(Long joinId, String irpPwd) {
		
		// 1) 가입건 조회
		IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow(
				() -> new IllegalArgumentException("가입건 없음: " + joinId));
		
		//이미 계좌 연결되어 있으면 중복 생성 방지
		if (join.getIrpAccount() != null) {
			IrpAccount acc = join.getIrpAccount();
			return new AccountContractResult(acc.getIrpAcctNo(), acc.getContractNo());
	    }
		
		// 완료 전 필수값 검증(예시)
        requireContractReady(join);
		
		//2) 계좌번호 생성
        String newAcctNo = accountNumberGenerator.next(); //예: "20250808-00001" (IRP계좌번호)
        String newContractNo = contractNumberGenerator.next(); // 예: IRP2025081100123 (IRP계약번호)
        
        //3) IRP 계좌 생성
        IrpAccount acct = IrpAccount.builder()
                .irpAcctNo(newAcctNo)
                .user(join.getUserId())
                .balance(BigDecimal.ZERO)
                .contractNo(newContractNo)
                .irpPwd(irpPwd)
                .status("ACTIVE")
                .build();
        irpAccountRepository.save(acct);
        
        // 4) 가입건에 계좌 연결
        join.setIrpAccount(acct);
        join.setContractNo(newContractNo);
        joinRepository.save(join);
		
		return new AccountContractResult(newAcctNo, newContractNo);
	}
	
	private void requireContractReady(IrpJoinEntity join){
        if (join.getAcctNo()==null) throw new IllegalStateException("출금계좌가 없습니다.");
        if (join.getAnnualContribAmt()==null) throw new IllegalStateException("연간 납입한도 미설정.");
        if (join.getNewContribAmt()==null) throw new IllegalStateException("신규 입금액 미설정.");
        if (join.getBranchOffice()==null) throw new IllegalStateException("관리 영업점 미설정.");
        // 목적별 필수 정보 존재 여부도 여기서 체크(예: 세액공제면 TaxPurpose 존재 등)
    }
}
