// src/main/java/com/example/memo/jpa/repository/purchase/commodity/FundMasterRepository.java
package com.example.memo.jpa.repository.purchase.commodity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

/**
 * FUND/ETF/TDF는 fund_master.category 컬럼으로만 구분한다.
 * - 커스텀 @Query 제거
 * - 접두어(F/E/T) 폴백 제거
 */
public interface FundMasterRepository extends JpaRepository<FundMaster, String> {

    /** 상태 멀티 조회(프로세스에서 사용 중이면 유지) */
    List<FundMaster> findByStatusIn(List<String> statuses);

    /** PK 조회(다른 서비스에서 이름 lookup 용으로 이미 사용 중) */
    Optional<FundMaster> findByProductId(String productId);

    /** 이름 부분검색(카테고리 무관, 필요 시 유지) */
    List<FundMaster> findByProductNameContainingIgnoreCase(String q);

    /** ---- 카테고리 전용 메서드 ---- */

    /** 카테고리 전체 (FUND|ETF|TDF) */
    List<FundMaster> findByCategoryIgnoreCase(String category);

    /** 카테고리 + 이름 검색 */
    List<FundMaster> findByCategoryIgnoreCaseAndProductNameContainingIgnoreCase(String category, String q);

    /** 카테고리 + 코드 검색 */
    List<FundMaster> findByCategoryIgnoreCaseAndProductIdContainingIgnoreCase(String category, String q);
    
    @Query("SELECT DISTINCT f " +
    	       "FROM FundMaster f " +
    	       "JOIN f.cumulativePerformances cp " +
    	       "WHERE f.category = :category " +
    	       "  AND f.riskGradeNum >= :riskGradeNum " +
    	       "  AND cp.periodCode = :periodCode " +
    	       "  AND cp.fundReturn IS NOT NULL " +
    	       "  AND cp.referenceDate = (" +
    	       "       SELECT MAX(c2.referenceDate) " +
    	       "       FROM CumulativePerformance c2 " +
    	       "       WHERE c2.fund = f " +
    	       "         AND c2.periodCode = cp.periodCode" +
    	       "  ) " +
    	       "ORDER BY cp.fundReturn DESC")
    	Page<FundMaster> findByCategoryAndRiskGradeNumAndPeriodCodeOrderByFundReturnDesc(
    	        @Param("category") String category,
    	        @Param("riskGradeNum") Integer riskGradeNum,
    	        @Param("periodCode") String periodCode,
    	        Pageable pageable
    	);
}
