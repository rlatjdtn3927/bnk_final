package com.example.memo.jpa.repository.admin;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.admin.BankEmployee;

public interface BankEmployeeRepository extends JpaRepository<BankEmployee, Long> {
    Optional<BankEmployee> findByUsername(String username);
}