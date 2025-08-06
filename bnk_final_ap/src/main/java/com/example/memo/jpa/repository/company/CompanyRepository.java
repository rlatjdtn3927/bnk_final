package com.example.memo.jpa.repository.company;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.company.Company;


public interface CompanyRepository extends JpaRepository<Company, Long>{
	
	boolean existsByBizNo(String bizNo);
}
