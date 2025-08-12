package com.example.memo.jpa.repository.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.company.DcDepositHistory;

@Repository
public interface DcDepositHistoryRepository extends JpaRepository<DcDepositHistory, Long> {

}
