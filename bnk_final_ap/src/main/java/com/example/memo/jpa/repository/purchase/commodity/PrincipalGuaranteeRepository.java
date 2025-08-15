package com.example.memo.jpa.repository.purchase.commodity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;

public interface PrincipalGuaranteeRepository extends JpaRepository<PrincipalGuarantee, String> {
	List<PrincipalGuarantee> findByStatusIn(List<String> asList);

	 /** 상품ID로 조회 */
    Optional<PrincipalGuarantee> findByProductId(String productId);

    /** 상품명 검색(대소문자 무시, 부분일치) */
    List<PrincipalGuarantee> findByProductNameContainingIgnoreCase(String q);
	}
