package com.example.memo.irp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.irp.entity.BankAccount;

public interface BankAccountRepository extends JpaRepository<BankAccount, String>{
	Optional<BankAccount> findByUserId(Long userId);
	
	Optional<BankAccount> findByAcctNoAndUserId(String acctNo, Long userId);
}
