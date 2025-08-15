package com.example.memo.jpa.repository.purchase.commodity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;

public interface PrincipalGuaranteeRepository extends JpaRepository<PrincipalGuarantee, String> {
	List<PrincipalGuarantee> findByStatusIn(List<String> asList);
}
