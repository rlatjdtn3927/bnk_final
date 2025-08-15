package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.PrincipalDocument;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

public interface PrincipalDocumentRepository extends JpaRepository<PrincipalDocument, Long> {
	List<PrincipalDocument> findByStatusIn(List<String> statuses);
	
	 /** 상품ID로 문서 목록 조회 */
    List<PrincipalDocument> findByPrincipal_ProductId(String productId);
}
