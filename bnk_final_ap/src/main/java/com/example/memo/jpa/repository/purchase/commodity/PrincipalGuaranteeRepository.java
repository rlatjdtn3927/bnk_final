package com.example.memo.jpa.repository.purchase.commodity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;

public interface PrincipalGuaranteeRepository extends JpaRepository<PrincipalGuarantee, String> {
	List<PrincipalGuarantee> findByStatusIn(List<String> asList);

	 /** 상품ID로 조회 */
    Optional<PrincipalGuarantee> findByProductId(String productId);

    /** 상품명 검색(대소문자 무시, 부분일치) */
    List<PrincipalGuarantee> findByProductNameContainingIgnoreCase(String q);
	
    /** 이름/코드 부분검색 (원리금보장 상품) */
    @Query("""
        select p from PrincipalGuarantee p
        where (:q = '' or upper(p.productName) like :qLike or upper(p.productId) like :qLike)
        order by p.productName asc
        """)
    List<PrincipalGuarantee> searchByQ(@Param("q") String q, @Param("qLike") String qLike);
}


