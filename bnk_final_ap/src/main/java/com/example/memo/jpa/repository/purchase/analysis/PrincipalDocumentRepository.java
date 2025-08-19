// src/main/java/com/example/memo/jpa/repository/purchase/analysis/PrincipalDocumentRepository.java
package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.memo.jpa.entity.purchase.analysis.PrincipalDocument;

@Repository
public interface PrincipalDocumentRepository extends JpaRepository<PrincipalDocument, Long> {

    List<PrincipalDocument> findByPrincipal_ProductId(String productId);

    // ✅ 배치 조회
    List<PrincipalDocument> findByPrincipal_ProductIdIn(List<String> productIds);

    // (과거 호환용) status는 Principal(연관객체)에 있으므로 JPQL로 명시
    @Query("select d from PrincipalDocument d where d.principal.status in :statuses")
    List<PrincipalDocument> findByStatusIn(@Param("statuses") List<String> statuses);
}
