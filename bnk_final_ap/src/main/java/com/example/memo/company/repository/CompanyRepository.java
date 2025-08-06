package com.example.memo.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.company.entity.Company;


public interface CompanyRepository extends JpaRepository<Company, Long>{
	
	boolean existsByBizNo(String bizNo);
}
