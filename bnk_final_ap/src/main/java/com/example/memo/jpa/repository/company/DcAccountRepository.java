package com.example.memo.jpa.repository.company;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.company.DcMember;

@Repository
public interface DcAccountRepository extends JpaRepository<DcAccount, Long> {

    /**
     * 특정 문자열로 시작하는 계좌번호의 개수를 셉니다.
     * (예: "DC-20250811" 로 시작하는 계좌 수 조회)
     * @param prefix 조회할 계좌번호의 접두사
     * @return 개수
     */
    long countByAccountNoStartingWith(String prefix);
    boolean existsByDcMember(DcMember dcMember);
    
    /**
     * 여러 가입자 ID에 해당하는 모든 DC 계좌 정보를 조회합니다.
     * @param memberIds 가입자 ID 목록
     * @return DcAccount 목록
     */
    List<DcAccount> findByDcMemberIdIn(List<Long> memberIds);
    
    Optional<DcAccount> findByDcMember_Id(Long memberId);
    
    @Query("SELECT a FROM DcAccount a WHERE a.dcMember.id IN :ids")
    List<DcAccount> findByMemberIds(@Param("ids") Set<Long> memberIds);

    @Query("SELECT a.dcMember.id, a.balance FROM DcAccount a WHERE a.dcMember.id IN :ids")
    List<Object[]> findBalancePairs(@Param("ids") Set<Long> memberIds);

    // DcAccount.dcMember.id 를 기준으로 조회
    List<DcAccount> findByDcMember_IdAndStatusNot(Long dcMemberId, String statusToExclude);
    
    /** userId로 해당 회원의 DC 계좌번호 리스트만 가볍게 조회 */
    @Query("""
           select a.accountNo
           from DcAccount a
           join a.dcMember m
           where m.user.userId = :userId
           """)
    List<String> findAccountNosByUserId(@Param("userId") Long userId);
}