package com.example.memo.irp.service;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.irp.BankAccount;
import com.example.memo.jpa.repository.irp.BankAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankAccountService {
	
	private final BankAccountRepository bankRepository;
	
	@Transactional(readOnly = true)
	public BankAccount getUserAccounts(Long userId){
		return bankRepository.findByUserId(userId).orElseThrow();
	}
	/*
	@Transactional(readOnly = true)
    public BigDecimal getAccountBalance(String acctNo) {
        BankAccount acct = bankRepository.findById(acctNo).orElseThrow();
        return acct.getBalance();
    }
	*/
	// 보안 검증 + 잔액 조회 (엔티티로)
    @Transactional(readOnly = true)
    public BigDecimal getBalanceForUser(String acctNo, Long userId) {
        return bankRepository.findByAcctNoAndUserId(acctNo, userId)
                .map(BankAccount::getBalance)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
    }
	
	/** 출금계좌 비밀번호 검증: 계좌가 로그인(또는 join의) 사용자 소유인지 + 비번 일치 확인 */
    @Transactional(readOnly = true)
    public boolean verifyAccountPassword(String acctNo, Long userId, String rawPwd) {
        return bankRepository.findByAcctNoAndUserId(acctNo, userId)
                .map(a -> Objects.equals(a.getAcctPwd(), rawPwd))
                .orElse(false);
    }
    
    /** 검증 실패 시 예외 던지는 버전(서비스 내부에서 쓰기 편함) */
    @Transactional(readOnly = true)
    public void verifyOrThrow(String acctNo, Long userId, String rawPwd) {
        if (!verifyAccountPassword(acctNo, userId, rawPwd)) {
            throw new IllegalArgumentException("출금계좌 비밀번호가 일치하지 않습니다.");
        }
    }
}
