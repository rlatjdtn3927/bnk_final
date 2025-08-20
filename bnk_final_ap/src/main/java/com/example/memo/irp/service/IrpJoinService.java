package com.example.memo.irp.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.irp.dto.AccountContractResult;
import com.example.memo.irp.dto.JoinSummaryResult;
import com.example.memo.irp.util.AccountNumberGenerator;
import com.example.memo.irp.util.ContractNumberGenerator;
import com.example.memo.jpa.entity.irp.BankAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.irp.IrpJoinEntity;
import com.example.memo.jpa.entity.irp.IrpRetirePurpose;
import com.example.memo.jpa.entity.irp.IrpTaxPurpose;
import com.example.memo.jpa.entity.irp.TestUserEntity;
import com.example.memo.jpa.repository.irp.BankAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.jpa.repository.irp.IrpJoinRepository;
import com.example.memo.jpa.repository.irp.IrpRetireRepository;
import com.example.memo.jpa.repository.irp.IrpTaxRepository;
import com.example.memo.jpa.repository.irp.TestUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IrpJoinService {

    private final BankAccountService bankAccountService;
    
	private final IrpJoinRepository joinRepository;
	private final IrpTaxRepository taxRepository;
	private final IrpRetireRepository retireRepository;
	private final IrpAccountRepository irpAccountRepository;
    private final BankAccountRepository bankRepository;
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
        Long userId = join.getUserId().getUserId();
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
	
	// ✅ step5 진입 시: 계약번호/계좌번호만 먼저 생성 (비번은 아직 X)
    @Transactional
    public AccountContractResult initOpen(Long joinId) {
    	AccountContractResult.AccountContractResultBuilder b = AccountContractResult.builder().joinId(joinId);
    	try {
    		
    		IrpJoinEntity join = joinRepository.findById(joinId)
    				.orElseThrow(() -> new IllegalArgumentException("가입건 없음: " + joinId));
    		
    		//requireContractReady(join); // step3 완료 등 사전조건 확인
    		
    		// 선행조건 체크: 예외 대신 false/코드로 처리
    		if (!isContractReady(join)) {
    			// 표식만 반환 (핸들러는 이 값으로 JSON 만들 수 있음)
    			return b.build(); // contractNo/irpAcctNo 미세팅
    		}
    		
    		// 계약번호 없으면 생성
    		String contractNo = (join.getContractNo() == null)
    				? contractNumberGenerator.next()
    						: join.getContractNo();
    		join.setContractNo(contractNo);
    		
    		// 계좌 없으면 발급(PENDING/INACTIVE), 비번은 아직 null
    		IrpAccount acct = (join.getIrpAccount() != null) ? join.getIrpAccount() : new IrpAccount();
    		if (acct.getIrpAcctNo() == null) {
    			acct.setIrpAcctNo(accountNumberGenerator.next());
    		}
    		acct.setUser(join.getUserId());
    		acct.setContractNo(contractNo);
    		if (acct.getStatus() == null || "ACTIVE".equals(acct.getStatus())) {
    			acct.setStatus("PENDING"); // ← 최종 완료 전 상태
    		}
    		// ❗ irpPwd는 아직 설정하지 않음 (null 허용 필요)
    		
    		irpAccountRepository.save(acct);
    		
    		join.setIrpAccount(acct);
    		joinRepository.save(join);
    		
    		return b.irpAcctNo(acct.getIrpAcctNo())
                    .contractNo(contractNo)
                    .build();
    	}catch (Exception e) {
            // ★ 어떤 예외도 밖으로 던지지 않음: AP는 항상 응답을 만들 수 있다
            // (로그만 남기고 빈 결과 반환)
            log.error("initOpen failed: {}", e.getMessage(), e);
            return b.build();
        }
    }
	
	//step5(최종완료):비밀번호 설정 + ACTIVE 
	@Transactional
	public AccountContractResult completeJoinAndOpenIrpAccount(Long joinId, String irpPwd) {
		// (선택) 동시성 제어: for update 로딩 또는 @Version 사용 권장
	    IrpJoinEntity join = joinRepository.findById(joinId)
	        .orElseThrow(() -> new IllegalArgumentException("가입건 없음: " + joinId));

	    //requireContractReady(join); // step3 완료 등 선행조건

	    // 비번 규칙 검사
	    if (irpPwd == null || !irpPwd.matches("\\d{4}")) {
	        throw new IllegalArgumentException("IRP 비밀번호는 4자리 숫자여야 합니다.");
	    }

	    IrpAccount acct = join.getIrpAccount();
	    if (acct == null) {
	        // initOpen 안 거치고 바로 complete 호출한 경우
	        throw new IllegalStateException("INIT_REQUIRED"); // 428 매핑 권장
	    }

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
	        // 비정상 상태 보호(정책에 맞게 409/422 등)
	        throw new IllegalStateException("INVALID_STATE");
	    }

	    // 운영 환경에서는 반드시 해시/솔트 저장
	    acct.setIrpPwd(irpPwd);
	    acct.setStatus("ACTIVE");
	    irpAccountRepository.save(acct);

	    // (join은 관계만 유지되면 save 생략 가능. 필요 시 save)
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
