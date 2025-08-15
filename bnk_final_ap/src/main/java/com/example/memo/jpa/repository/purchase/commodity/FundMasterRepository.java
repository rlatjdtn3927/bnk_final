package com.example.memo.jpa.repository.purchase.commodity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

public interface FundMasterRepository extends JpaRepository<FundMaster, String> {
    List<FundMaster> findByStatusIn(List<String> statuses);

}
