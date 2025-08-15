// src/main/java/com/example/memo/jpa/repository/purchase/analysis/PrincipalDocumentRepository.java
package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.purchase.analysis.PrincipalDocument;

/**
 * Principal(원리금보장 상품) 관련 문서 저장소.
 * - 주의: PrincipalDocument 엔티티에는 status 필드가 없습니다.
 * - status는 연관 상품(PrincipalGuarantee)에 존재합니다.
 *   따라서 status로 필터링할 때는 d.principal.status를 사용해야 합니다.
 */
@Repository
public interface PrincipalDocumentRepository extends JpaRepository<PrincipalDocument, Long> {

    /** 특정 상품(productId)의 문서 전부 조회 */
    List<PrincipalDocument> findByPrincipal_ProductId(String productId);

    /** 여러 상품(productId) 묶음으로 문서 조회 */
    List<PrincipalDocument> findByPrincipal_ProductIdIn(List<String> productIds);

    /**
     * 🔧핫픽스: 기존에 메서드 이름만 보고 파생쿼리를 만들면
     * "No property 'status' found for type 'PrincipalDocument'" 에러가 발생합니다.
     * status는 PrincipalDocument가 아니라 PrincipalGuarantee에 있으므로,
     * JPQL로 명시적으로 d.principal.status를 조건으로 잡아줍니다.
     *
     * ApprovalService 에서 기존에 사용하던 시그니처(메서드명/파라미터)는 그대로 둡니다.
     */
    @Query("select d from PrincipalDocument d where d.principal.status in :statuses")
    List<PrincipalDocument> findByStatusIn(@Param("statuses") List<String> statuses);
}
