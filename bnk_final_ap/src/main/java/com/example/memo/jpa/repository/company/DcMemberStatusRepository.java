package com.example.memo.jpa.repository.company;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.company.DcMemberStatus;

@Repository
public interface DcMemberStatusRepository extends JpaRepository<DcMemberStatus, Long> {

    // 특정 가입자의 상태 목록
    List<DcMemberStatus> findByDcMemberId(Long dcMemberId);
    
    /** 멤버별 최신 상태 한 건씩 (createdAt/ id 기준) */
    @Query("""
           SELECT s.dcMember.id, s.status
           FROM DcMemberStatus s
           WHERE s.id IN (
             SELECT MAX(s2.id) FROM DcMemberStatus s2
             WHERE s2.dcMember.id IN :ids
             GROUP BY s2.dcMember.id
           )
           """)
    List<Object[]> findLatestStatusPairs(@Param("ids") Set<Long> memberIds);
}