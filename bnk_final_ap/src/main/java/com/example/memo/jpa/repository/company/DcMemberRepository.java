package com.example.memo.jpa.repository.company;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.company.DcMember;

@Repository
public interface DcMemberRepository extends JpaRepository<DcMember, Long> {

    // 회사 ID로 가입자 목록 조회
    List<DcMember> findByCompanyId(Long companyId);

    // 주민등록번호 중복 체크
    boolean existsByRrn(String rrn);
}