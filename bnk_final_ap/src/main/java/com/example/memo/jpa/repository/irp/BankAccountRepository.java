package com.example.memo.jpa.repository.irp;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.memo.jpa.entity.irp.BankAccount;

import jakarta.persistence.LockModeType;

public interface BankAccountRepository extends JpaRepository<BankAccount, String>{
	Optional<BankAccount> findByUserId(Long userId);
	
	Optional<BankAccount> findByAcctNoAndUserId(String acctNo, Long userId);
	
	// 완료 시 "동시에 이체 + 멱등"
	@Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BankAccount b where b.acctNo = :acctNo")
    Optional<BankAccount> findForUpdate(@Param("acctNo") String acctNo);
}
