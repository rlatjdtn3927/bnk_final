package com.example.memo.jpa.repository.company;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.company.CompanyManager;

@Repository
public interface CompanyManagerRepository extends JpaRepository<CompanyManager, Long> {
    Optional<CompanyManager> findByLoginId(String loginId);
}

