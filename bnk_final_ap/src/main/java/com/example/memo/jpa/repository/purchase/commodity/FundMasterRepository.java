package com.example.memo.jpa.repository.purchase.commodity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
//펀드 리포지토리
public interface FundMasterRepository extends JpaRepository<FundMaster, String> {
    List<FundMaster> findByStatusIn(List<String> statuses);
    /** 상품ID로 조회 */
    Optional<FundMaster> findByProductId(String productId);

    /** 상품명 검색(대소문자 무시, 부분일치) */
    List<FundMaster> findByProductNameContainingIgnoreCase(String q);

    /**
     * FUND/ETF/TDF 를 FundMaster 테이블에서 분리 조회.
     * - useCat=true  : category 컬럼으로 필터(upper(f.category)=:cat)
     * - useCat=false : productId prefix(F/E/T)로 필터(upper(f.productId) like :prefix)
     * - qLike        : 이름/코드 부분검색
     *
     * ※ 실제 컬럼명(category/productName/productId)은 프로젝트 스키마 기준.
     */
    @Query("""
        select f from FundMaster f
        where
          (
            (:useCat = true and upper(f.category) = :cat)
            or
            (:useCat = false and upper(f.productId) like :prefix)
          )
          and (
            :q = '' or upper(f.productName) like :qLike or upper(f.productId) like :qLike
          )
        order by f.productName asc
        """)
    List<FundMaster> searchByCategoryOrPrefix(
            @Param("useCat") boolean useCategoryColumn,
            @Param("cat") String categoryUpper,   // "FUND"|"ETF"|"TDF"
            @Param("prefix") String idPrefixLike, // "F%"|"E%"|"T%"
            @Param("q") String qRaw,
            @Param("qLike") String qLike
    );
}
