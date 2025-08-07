package com.example.memo.jpa.repository.company;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.company.DcMemberStatus;

@Repository
public interface DcMemberStatusRepository extends JpaRepository<DcMemberStatus, Long> {

    // 특정 가입자의 상태 목록
    List<DcMemberStatus> findByDcMemberId(Long dcMemberId);
}