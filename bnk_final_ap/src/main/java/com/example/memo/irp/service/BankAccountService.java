package com.example.memo.irp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.irp.entity.BankAccount;
import com.example.memo.irp.repository.BankAccountRepository;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankAccountService {
	
	private final BankAccountRepository bankRepository;
	
	@Transactional(readOnly = true)
	public BankAccount getUserAccounts(Long userId){
		return bankRepository.findByUserId(userId).orElseThrow();
	}
	
	@Transactional(readOnly = true)
    public Long getAccountBalance(String acctNo) {
        BankAccount acct = bankRepository.findById(acctNo).orElseThrow();
        return acct.getBalance().longValue();
    }
}
