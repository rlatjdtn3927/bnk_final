package com.example.memo.jpa.repository.irp;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.irp.BankAccount;

public interface BankAccountRepository extends JpaRepository<BankAccount, String>{
	Optional<BankAccount> findByUserId(Long userId);
	
	Optional<BankAccount> findByAcctNoAndUserId(String acctNo, Long userId);
}
