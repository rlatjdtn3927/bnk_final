package com.example.memo.jpa.repository.company;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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


}