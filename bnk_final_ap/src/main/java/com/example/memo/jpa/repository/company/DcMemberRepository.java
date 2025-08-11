package com.example.memo.jpa.repository.company;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.memo.company.dto.SubscriberListDto;
import com.example.memo.jpa.entity.company.DcMember;

@Repository
public interface DcMemberRepository extends JpaRepository<DcMember, Long> {

	boolean existsByRrn(String rrn);

    @Query("SELECT m FROM DcMember m LEFT JOIN FETCH m.dcMemberStatus s WHERE m.companyId = :companyId")
    List<DcMember> findMembersWithStatusByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT m FROM DcMember m JOIN FETCH m.dcMemberStatus s WHERE m.companyId = :companyId AND s.status = :status")
    List<DcMember> findMembersByStatus(@Param("companyId") Long companyId, @Param("status") String status);
    
    // 이름으로 검색
    List<DcMember> findByCompanyIdAndNameContaining(Long companyId, String name);
    
    // 이름 + 재직상태로 검색
    @Query("SELECT m FROM DcMember m JOIN m.dcMemberStatus s WHERE m.companyId = :companyId AND m.name LIKE %:name% AND s.status = :status")
    List<DcMember> findByCompanyIdAndNameAndStatus(@Param("companyId") Long companyId, 
                                                   @Param("name") String name, 
                                                   @Param("status") String status);
}