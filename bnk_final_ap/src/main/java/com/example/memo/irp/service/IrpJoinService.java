package com.example.memo.irp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    
	
    @Transactional
    public IrpJoinEntity findByIdOrThrow(Long joinId) {
		return joinRepository.findById(joinId).orElseThrow();
	}
    
	@Transactional
	public IrpJoinEntity saveJoin(IrpJoinEntity irpJoinEntity) {
		return joinRepository.save(irpJoinEntity);
	}
	
	// 가입 가능 여부 : ACTIVE만 차단
	@Transactional(readOnly = true)
	public boolean hasActiveIrp(Long userId) {
	    return irpAccountRepository.existsByUser_UserIdAndStatus(userId, "ACTIVE");
	}
	
	//step1 : 가입목적 선택(userId, joinPurpose만 저장)
	@Transactional
    public Long createDraft(Long userId, String joinPurpose) {
		System.out.println(userId);
        UserEntity user = userRepository.findById(userId).orElseThrow();
        
        // 1) 이미 진행중인 DRAFT/PENDING이 있으면 그걸 바로 재사용 (joinId 변하지 않음)
        Optional<IrpJoinEntity> existing = joinRepository
                .findTopByUser_UserIdAndStatusInOrderByRegDateDesc(
                    userId, List.of("DRAFT", "PENDING"));
        
        if (existing.isPresent()) {
            IrpJoinEntity j = existing.get();
            // 목적이 바뀌었다면 진행중 편집으로 간주해 업데이트(선택)
            if (joinPurpose != null && !joinPurpose.isBlank() &&
                !joinPurpose.equals(j.getJoinPurpose())) {
                j.setJoinPurpose(joinPurpose);
                joinRepository.save(j);
            }
            return j.getJoinId(); // ★ 기존 joinId 그대로 반환
        }
        
        // 2) 없으면 새로 생성 (DRAFT)
        IrpJoinEntity join = IrpJoinEntity.builder()
                .user(user)
                .joinPurpose(joinPurpose)
                .status("DRAFT")
                .newAmtApplied(false)
                .build();
        return joinRepository.save(join).getJoinId();
    }
	
	//가입목적 - 세액공제
	@Transactional
	public void saveTaxPurpose(Long joinId, String qualType, String busiNo) {
		IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow();
		
		IrpTaxPurpose tax = taxRepository.findById(joinId).orElse(null);
	    if (tax == null) {
	        tax = IrpTaxPurpose.builder().join(join).build();
	    }
	    tax.setIrpQualType(qualType);
	    tax.setBusinessNo(busiNo);
	    taxRepository.save(tax);
	}
		
	//가입목적 - 퇴직금 수령
	@Transactional
	public void saveRetirePurpose(Long joinId, LocalDate retireDate, String retireReason, String corpName, Long severAmt, String withholdDoc) {
		IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow();
		
		// NOT NULL 보장(엔티티 제약과 맞춤)
	    if (retireDate == null) throw new IllegalArgumentException("retireDate required");
	    if (corpName == null || corpName.isBlank()) throw new IllegalArgumentException("corpName required");
	    if (severAmt == null) throw new IllegalArgumentException("severanceAmt required");

	    IrpRetirePurpose retire = retireRepository.findById(joinId).orElse(null);
	    if (retire == null) {
	        retire = IrpRetirePurpose.builder().join(join).build();
	    }
	    retire.setRetireDate(retireDate);
	    retire.setRetireReason(retireReason);
	    retire.setCorpName(corpName);
	    retire.setSeveranceAmt(severAmt);
	    retire.setWithholdDoc(withholdDoc);
	    
		retireRepository.save(retire);
	}
	
	//step3: 계약정보 업데이트(부분 저장)
    @Transactional
    public void updateContract(Long joinId, Long annualAmt, Long newAmt,
                               String acctNo, String acctPwd, String branchOffice) {
        IrpJoinEntity join = joinRepository.findById(joinId).orElseThrow();
        
        // 계좌 소유자/비번 검증까지만(돈은 아직 움직이지 않음)
        Long userId = join.getUser().getUserId();
        bankAccountService.verifyOrThrow(acctNo, userId, acctPwd);	// 비밀번호 틀리면 즉시 예외 → 저장 차단
        
        // 값 검증(선택)
        if (annualAmt != null && annualAmt < 0) 
        	throw new IllegalArgumentException("annualContribAmt 음수 불가");
        if (newAmt != null && newAmt < 0) 
        	throw new IllegalArgumentException("newContribAmt 음수 불가");
        
        // 계좌 엔티티 로드 (userId로 출금계좌 찾기)
        BankAccount acct = bankRepository.findById(acctNo).orElseThrow();
        
        join.setAnnualContribAmt(annualAmt);
        join.setNewContribAmt(newAmt);
        join.setAcctNo(acct);
        join.setBranchOffice(branchOffice);
        
        // 상태는 계속 DRAFT
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
        String raw = v.toString(); // 예: 2025082300001

        // 앞 4-중간 4-나머지로 포맷
        String formatted = raw.substring(0,4) + "-" 
                         + raw.substring(4,8) + "-" 
                         + raw.substring(8);
        return formatted;
    }
	
	// step5: 계약번호/계좌번호만 먼저 생성 (비번은 아직 X)
    @Transactional
    public InitOpenResponse initOpen(Long joinId) {
    	IrpJoinEntity join = joinRepository.findById(joinId)
    	        .orElseThrow(() -> new IllegalArgumentException("가입건 없음: " + joinId));
    	
    	// 1) 계약번호 없으면 새로 생성
    	if (join.getContractNo() == null || join.getContractNo().isBlank()) {
            join.setContractNo(nextContractNo());      // 오라클 시퀀스 호출
        }
    	
    	// 2) 사용자 기존 IRP 계좌 먼저 조회(멱등)
        IrpAccount acct = irpAccountRepository.findByUser_UserId(join.getUser().getUserId())
        		.orElse(null);
        // 3)
        if (acct == null) {
            // 새 계좌 생성 시도
            acct = IrpAccount.builder()
                    .irpAcctNo(nextIrpAcctNo())
                    .user(join.getUser())
                    .contractNo(join.getContractNo())
                    .status("PENDING")
                    .build();
            try {
                acct = irpAccountRepository.saveAndFlush(acct);
            } catch (Exception dup) {
                // 동시성으로 유니크 위반 등 발생 시 재조회로 멱등 보장
                acct = irpAccountRepository.findByUser_UserId(join.getUser().getUserId())
                        .orElseThrow();
            }
        } else {
            // 기존 계좌가 있으면 계약번호 동기화만 (필요 시)
            if (acct.getContractNo() == null) {
                acct.setContractNo(join.getContractNo());
                irpAccountRepository.save(acct);
            }
        }
        
        join.setIrpAccount(acct);
        
        joinRepository.save(join);
        //em.flush();
        
        return new InitOpenResponse(join.getContractNo(), acct.getIrpAcctNo());
    }
	
	//step5(최종완료):비밀번호 설정 + ACTIVE 
	@Transactional
	public AccountContractResult completeJoinAndOpenIrpAccount(Long joinId, String irpPwd) {
	    IrpJoinEntity join = joinRepository.findById(joinId)
	        .orElseThrow(() -> new IllegalArgumentException("가입건 없음: " + joinId));

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
	    
	    // ★ 동시에 이체 (한 트랜잭션)
	    Long newAmt = join.getNewContribAmt();
	    if (!Boolean.TRUE.equals(join.isNewAmtApplied()) && newAmt != null && newAmt > 0) {

	        // 1) 출금계좌/IRP계좌 비관적 잠금
	        BankAccount debit = bankRepository.findForUpdate(
	                join.getAcctNo().getAcctNo()).orElseThrow();
	        IrpAccount credit = irpAccountRepository.findForUpdate(
	                acct.getIrpAcctNo()).orElseThrow();

	        BigDecimal amt = BigDecimal.valueOf(newAmt);

	        // 2) 잔액 체크
	        if (debit.getBalance().compareTo(amt) < 0) {
	            throw new IllegalStateException("계좌 잔액 부족");
	        }

	        // 3) 출금 → 입금
	        debit.setBalance(debit.getBalance().subtract(amt));
	        if (credit.getBalance() == null) credit.setBalance(BigDecimal.ZERO);
	        credit.setBalance(credit.getBalance().add(amt));

	        // 4) 멱등 플래그 ON (다음 번엔 재이체 금지)
	        join.setNewAmtApplied(true);
	    }
	    

	    // 비번 설정 + 활성화
	    acct.setIrpPwd(irpPwd);
	    acct.setStatus("ACTIVE");
	    
	    irpAccountRepository.save(acct);
	    join.setStatus("ACTIVE"); // 조인도 완료 처리
	    joinRepository.save(join);

	    return AccountContractResult.builder()
	            .joinId(joinId)
	            .irpAcctNo(acct.getIrpAcctNo())
	            .contractNo(acct.getContractNo())
	            .build();
	}

}
