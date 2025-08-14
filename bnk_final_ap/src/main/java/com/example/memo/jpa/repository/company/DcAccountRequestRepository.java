package com.example.memo.jpa.repository.company;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.company.DcAccountRequest;
import com.example.memo.jpa.entity.company.DcMember;

@Repository
public interface DcAccountRequestRepository extends JpaRepository<DcAccountRequest, Long> {


    /**
     * 특정 상태(status)에 해당하는 모든 계좌 개설 요청을 조회합니다.
     * 예: "PENDING" 상태인 요청만 모두 가져올 때 사용합니다.
     * @param status 조회할 요청 상태
     * @return 해당 상태의 요청 목록
     */
    List<DcAccountRequest> findByStatus(String status);
    boolean existsByDcMemberAndStatus(DcMember dcMember, String status);
    /**
     * 여러 가입자 ID와 특정 상태에 해당하는 모든 요청 정보를 조회합니다.
     * @param memberIds 가입자 ID 목록
     * @param status 조회할 상태 (예: "PENDING")
     * @return DcAccountRequest 목록
     */
    List<DcAccountRequest> findByDcMemberIdInAndStatus(List<Long> memberIds, String status);
    List<DcAccountRequest> findByStatusOrderByRequestedAtDesc(String status);
}