package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.FundDocument;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

public interface FundDocumentRepository extends JpaRepository<FundDocument, Long> {
	List<FundDocument> findByStatusIn(List<String> statuses);
}
