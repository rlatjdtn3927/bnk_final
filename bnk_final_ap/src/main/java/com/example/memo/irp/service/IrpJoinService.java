package com.example.memo.irp.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.irp.dto.AccountContractResult;
import com.example.memo.irp.dto.InitOpenResponse;
import com.example.memo.irp.dto.JoinSummaryResult;
//import com.example.memo.irp.util.AccountNumberGenerator;
//import com.example.memo.irp.util.ContractNumberGenerator;
import com.example.memo.jpa.entity.irp.BankAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.irp.IrpJoinEntity;
import com.example.memo.jpa.entity.irp.IrpRetirePurpose;
import com.example.memo.jpa.entity.irp.IrpTaxPurpose;
import com.example.memo.jpa.entity.user.UserEntity;
import com.example.memo.jpa.repository.irp.BankAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.jpa.repository.irp.IrpJoinRepository;
import com.example.memo.jpa.repository.irp.IrpRetireRepository;
import com.example.memo.jpa.repository.irp.IrpTaxRepository;
import com.example.memo.jpa.repository.user.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final UserRepository userRepository;
    
//    private final AccountNumberGenerator accountNumberGenerator; // 커스텀 유틸
//    private final ContractNumberGenerator contractNumberGenerator;
	
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
        UserEntity user = userRepository.findById(userId).orElseThrow();
        IrpJoinEntity join = IrpJoinEntity.builder()
                .user(user)
                .joinPurpose(joinPurpose)
                .build();
        return joinRepository.save(join).getJoinId();
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
	
	//step3: 계약정보 업데이트(부분 저장)
    @Transactional
    public void updateContract(Long joinId, Long annualAmt, Long newAmt,
                               String acctNo, String acctPwd, String branchOffice) {
        IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow();
        
        //1) 계좌 비번 검증 (계좌 소유자 = join.userId)
        Long userId = join.getUser().getUserId();
        bankAccountService.verifyOrThrow(acctNo, userId, acctPwd);	// 비밀번호 틀리면 즉시 예외 → 저장 차단
        
        //2) 계좌 엔티티 로드 (userId로 출금계좌 찾기)
        BankAccount acct = bankRepository.findById(acctNo).orElseThrow();
        
        //3) 값 검증(선택)
        if (annualAmt != null && annualAmt < 0) 
        	throw new IllegalArgumentException("annualContribAmt 음수 불가");
        if (newAmt != null && newAmt < 0) 
        	throw new IllegalArgumentException("newContribAmt 음수 불가");
        
        BigDecimal balance = acct.getBalance(); // 현재 잔액 (BigDecimal)
        BigDecimal newAmtBd = BigDecimal.valueOf(newAmt);//신규 입금 금액 (Long → BigDecimal)
        
        //4) 신규 입금 금액만큼 계좌 잔액 차감
        if (balance.compareTo(newAmtBd) < 0) {
            throw new IllegalStateException("계좌 잔액 부족");
        }
        // 잔액차감
        acct.setBalance(balance.subtract(newAmtBd));
        
        join.setAnnualContribAmt(annualAmt);
        join.setNewContribAmt(newAmt);
        join.setAcctNo(acct);
        join.setBranchOffice(branchOffice);
        // save 생략 가능(영속상태)
        joinRepository.save(join);
    }
    
    //step3: 조회
    @Transactional(readOnly = true)
    public JoinSummaryResult getContract(Long joinId) { 
        IrpJoinEntity j = joinRepository.findById(joinId)
            .orElseThrow(() -> new IllegalArgumentException("가입건 없음: " + joinId));

        return JoinSummaryResult.builder()
                .joinId(j.getJoinId())
                .branchOffice(j.getBranchOffice())
                .annualContribAmt(j.getAnnualContribAmt())
                .newContribAmt(j.getNewContribAmt())
                .build();
    }
	
	//step4: 가입정보확인
	@Transactional(readOnly = true)
	public JoinSummaryResult prepareOpen(Long joinId) {
		IrpJoinEntity join = joinRepository.findById(joinId)
		        .orElseThrow(() -> new IllegalArgumentException("가입건 없음: " + joinId));

		return new JoinSummaryResult(
			join.getJoinId(),
	        join.getBranchOffice(),
	        join.getAnnualContribAmt(), // Long
	        join.getNewContribAmt()     // Long
	    );
	}
	
	// 오라클 EM만 사용 (프로젝트에 oracleEmf가 있으면 unitName 지정, 없으면 기본 @PersistenceContext만)
    @PersistenceContext(unitName = "oracleEmf")
    private EntityManager em;

    /** 오라클에서 계약번호: IRP + YYYYMMDD + 5자리 */
    private String nextContractNo() {
        Object v = em.createNativeQuery(
            "SELECT 'IRP' || TO_CHAR(SYSDATE,'YYYYMMDD') || LPAD(IRP_CONTRACT_NO_SEQ.NEXTVAL, 5, '0') FROM DUAL"
        ).getSingleResult();
        return v.toString();
    }

    /** 오라클에서 계좌번호: YYYYMMDD + 5자리 */
    private String nextIrpAcctNo() {
        Object v = em.createNativeQuery(
            "SELECT TO_CHAR(SYSDATE,'YYYYMMDD') || LPAD(IRP_ACCT_NO_SEQ.NEXTVAL, 5, '0') FROM DUAL"
        ).getSingleResult();
        return v.toString();
    }
	
	// step5: 계약번호/계좌번호만 먼저 생성 (비번은 아직 X)
    @Transactional
    public InitOpenResponse initOpen(Long joinId) {
    	IrpJoinEntity join = joinRepository.findById(joinId)
    	        .orElseThrow(() -> new IllegalArgumentException("가입건 없음: " + joinId));
    	
    	// 계약번호 없으면 새로 생성
    	if (join.getContractNo() == null || join.getContractNo().isBlank()) {
            join.setContractNo(nextContractNo());      // 오라클 시퀀스 호출
        }
        
        // 계좌 없으면 새로 생성
    	IrpAccount acct = (join.getIrpAccount() != null) ? join.getIrpAccount() : new IrpAccount();
        if (acct.getIrpAcctNo() == null || acct.getIrpAcctNo().isBlank()) {
            acct.setIrpAcctNo(nextIrpAcctNo());        // 오라클 시퀀스 호출
        }
        acct.setUser(join.getUser());
        acct.setContractNo(join.getContractNo());
        if (acct.getStatus() == null) {
            acct.setStatus("PENDING");
        }
        
        acct = irpAccountRepository.save(acct);
        
        join.setIrpAccount(acct);
        
        joinRepository.save(join);
        em.flush();
        
        return new InitOpenResponse(join.getContractNo(), acct.getIrpAcctNo());
    }
	
	//step5(최종완료):비밀번호 설정 + ACTIVE 
	@Transactional
	public AccountContractResult completeJoinAndOpenIrpAccount(Long joinId, String irpPwd) {
	    IrpJoinEntity join = joinRepository.findById(joinId)
	        .orElseThrow(() -> new IllegalArgumentException("가입건 없음: " + joinId));

	    //requireContractReady(join); // step3 완료 등 선행조건

	    // 비번 규칙 검사
	    if (irpPwd == null || !irpPwd.matches("\\d{4}")) {
	        throw new IllegalArgumentException("IRP 비밀번호는 4자리 숫자여야 합니다.");
	    }

	    IrpAccount acct = join.getIrpAccount();
	    if (acct == null) throw new IllegalStateException("INIT_REQUIRED");

	    // 멱등 처리: 이미 활성화면 그대로 반환
	    if ("ACTIVE".equals(acct.getStatus())) {
	        return AccountContractResult.builder()
	                .joinId(joinId)
	                .irpAcctNo(acct.getIrpAcctNo())
	                .contractNo(acct.getContractNo())
	                .build();
	    }

	    // PENDING -> ACTIVE 전환
	    if (!"PENDING".equals(acct.getStatus())) {
	        throw new IllegalStateException("INVALID_STATE");
	    }
	    
	    // 여기서 신규 납입금액을 IRP 계좌에 ‘한 번만’ 반영
	    Long newAmt = join.getNewContribAmt();                 // step3에서 저장해둔 금액
	    if (newAmt != null && newAmt > 0) {
	        BigDecimal add = BigDecimal.valueOf(newAmt);
	        if (acct.getBalance() == null) acct.setBalance(BigDecimal.ZERO);
	        acct.setBalance(acct.getBalance().add(add));
	    }

	    // 비번 설정 + 활성화
	    acct.setIrpPwd(irpPwd);
	    acct.setStatus("ACTIVE");
	    irpAccountRepository.save(acct);

	    joinRepository.save(join);

	    return AccountContractResult.builder()
	            .joinId(joinId)
	            .irpAcctNo(acct.getIrpAcctNo())
	            .contractNo(acct.getContractNo())
	            .build();
	}
	
	private boolean isContractReady(IrpJoinEntity join) {
	    try {
	        requireContractReady(join); // 기존 검증 사용
	        return true;
	    } catch (Exception ex) {
	        return false;
	    }
	}
	
	private void requireContractReady(IrpJoinEntity join){
        if (join.getAcctNo()==null) throw new IllegalStateException("출금계좌가 없습니다.");
        if (join.getAnnualContribAmt()==null) throw new IllegalStateException("연간 납입한도 미설정.");
        if (join.getNewContribAmt()==null) throw new IllegalStateException("신규 입금액 미설정.");
        if (join.getBranchOffice()==null) throw new IllegalStateException("관리 영업점 미설정.");
        // 목적별 필수 정보 존재 여부도 여기서 체크(예: 세액공제면 TaxPurpose 존재 등)
    }
	/*
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
	 */
}
