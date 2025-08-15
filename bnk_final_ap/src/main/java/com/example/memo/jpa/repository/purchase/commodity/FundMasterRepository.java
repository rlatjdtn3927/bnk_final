package com.example.memo.jpa.repository.purchase.commodity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
//펀드 리포지토리
public interface FundMasterRepository extends JpaRepository<FundMaster, String> {
    List<FundMaster> findByStatusIn(List<String> statuses);
    /** 상품ID로 조회 */
    Optional<FundMaster> findByProductId(String productId);

    /** 상품명 검색(대소문자 무시, 부분일치) */
    List<FundMaster> findByProductNameContainingIgnoreCase(String q);
}
