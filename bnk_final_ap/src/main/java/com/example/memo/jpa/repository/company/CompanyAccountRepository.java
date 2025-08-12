package com.example.memo.jpa.repository.company;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.memo.jpa.entity.company.CompanyAccount;

public interface CompanyAccountRepository extends JpaRepository<CompanyAccount, Long> {

    // 회사의 모든 계좌 조회 (status 없음)
    @Query("select a from CompanyAccount a where a.company.id = :companyId")
    List<CompanyAccount> findByCompanyId(@Param("companyId") Long companyId);

    // 회사 소유의 특정 계좌 조회
    @Query("select a from CompanyAccount a where a.id = :id and a.company.id = :companyId")
    Optional<CompanyAccount> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}