package com.example.memo.jpa.repository.company;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.memo.company.dto.PayableItemDto;
import com.example.memo.jpa.entity.company.DcContributionItem;
import com.example.memo.jpa.entity.company.DcContributionItem.PaymentStatus;

@Repository
public interface DcContributionItemRepository extends JpaRepository<DcContributionItem, Long> {

    /**
     * 입금 대상 항목 조회 쿼리
     * 배치의 상태가 아닌, 각 항목의 paymentStatus가 PENDING인 것을 조건으로 조회
     */
    @Query("SELECT new com.example.memo.company.dto.PayableItemDto(" +
            "  i.id, b.planDate, m.name, a.accountNo, i.amount, i.paymentStatus" +
            ") " +
            "FROM DcContributionItem i " +
            "JOIN i.batch b " +
            "JOIN i.dcMember m " +
            "JOIN DcAccount a ON a.dcMember = m " +
            "WHERE b.company.id = :companyId " +
            "  AND b.planDate BETWEEN :from AND :to " +
            "  AND i.paymentStatus IN :statuses " +
            "ORDER BY " +
            "  CASE WHEN i.paymentStatus = com.example.memo.jpa.entity.company.DcContributionItem.PaymentStatus.PENDING THEN 1 " +
            "       WHEN i.paymentStatus = com.example.memo.jpa.entity.company.DcContributionItem.PaymentStatus.PAID THEN 2 " +
            "       ELSE 3 END ASC, " +
            "  b.planDate DESC"
     )
    Page<PayableItemDto> findPayableItemsWithStatus(
            @Param("companyId") Long companyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") List<PaymentStatus> statuses,
            Pageable pageable
    );
}
