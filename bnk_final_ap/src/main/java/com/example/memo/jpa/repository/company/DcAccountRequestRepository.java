package com.example.memo.jpa.repository.company;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.company.DcAccountRequest;

@Repository
public interface DcAccountRequestRepository extends JpaRepository<DcAccountRequest, Long> {


    /**
     * 특정 상태(status)에 해당하는 모든 계좌 개설 요청을 조회합니다.
     * 예: "PENDING" 상태인 요청만 모두 가져올 때 사용합니다.
     * @param status 조회할 요청 상태
     * @return 해당 상태의 요청 목록
     */
    List<DcAccountRequest> findByStatus(String status);

}