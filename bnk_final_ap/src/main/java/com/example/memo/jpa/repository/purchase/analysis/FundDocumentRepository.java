package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.FundDocument;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

public interface FundDocumentRepository extends JpaRepository<FundDocument, Long> {
	// (기존) 상태 조회 — 다른 곳에서 사용 중이면 유지
    List<FundDocument> findByStatusIn(List<String> statuses);

    // (기존) 단건 조회
    List<FundDocument> findByFund_ProductId(String productId);

    List<FundDocument> findByFund_ProductIdIn(List<String> productIds);
}
