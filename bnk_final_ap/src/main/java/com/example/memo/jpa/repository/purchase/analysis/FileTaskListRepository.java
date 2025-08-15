// src/main/java/com/example/memo/jpa/repository/purchase/analysis/FileTaskListRepository.java
package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.purchase.analysis.FileTaskList;

/**
 * 파일 작업 리스트 레포지토리
 *
 * - 엔티티 구조는 고정 (prodId, prodName, prodCategory, docType, downloadUrl, fileSize)
 * - ApprovalService 쪽에서 기대하는 메서드 시그니처(findByProductIdIn)를 유지하되,
 *   실제 컬럼명(prodId)에 맞게 JPQL로 구현한다.
 */
@Repository
public interface FileTaskListRepository extends JpaRepository<FileTaskList, Long> {

    /**
     * 기존 ApprovalService가 호출하는 시그니처를 그대로 유지
     * - 컬럼명은 productId가 아니라 `prodId` 이므로 JPQL에서 prodId로 조회
     */
    @Query("select f from FileTaskList f where f.prodId in :ids")
    List<FileTaskList> findByProductIdIn(@Param("ids") List<String> ids);

    // 필요시 확장용 (카테고리/문서타입 필터링)
    @Query("""
           select f
             from FileTaskList f
            where f.prodId in :ids
              and (:categories is null or lower(f.prodCategory) in :categories)
              and (:docTypes is null   or lower(f.docType) in :docTypes)
           """)
    List<FileTaskList> findByProdIdInWithFilters(@Param("ids") List<String> ids,
                                                 @Param("categories") List<String> categoriesLowerOrNull,
                                                 @Param("docTypes") List<String> docTypesLowerOrNull);

    // 심플 파생쿼리도 함께 제공(다른 서비스에서 재사용 가능)
    List<FileTaskList> findByProdIdIn(List<String> prodIds);
}
