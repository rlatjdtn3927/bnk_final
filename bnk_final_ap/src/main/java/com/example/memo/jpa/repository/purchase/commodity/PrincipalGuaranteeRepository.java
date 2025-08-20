// src/main/java/com/example/memo/jpa/repository/purchase/commodity/PrincipalGuaranteeRepository.java
package com.example.memo.jpa.repository.purchase.commodity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;

public interface PrincipalGuaranteeRepository extends JpaRepository<PrincipalGuarantee, String> {

    /** 상태 멀티 조회(프로세스에서 사용 중이면 유지) */
    List<PrincipalGuarantee> findByStatusIn(List<String> statuses);

    /** PK 조회 */
    Optional<PrincipalGuarantee> findByProductId(String productId);

    /** 이름 부분검색 */
    List<PrincipalGuarantee> findByProductNameContainingIgnoreCase(String q);

    /** 코드 부분검색 */
    List<PrincipalGuarantee> findByProductIdContainingIgnoreCase(String q);
    
    Page<PrincipalGuarantee> findAll(Pageable pageable);
}
