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
import com.example.memo.jpa.entity.company.DcContributionBatch;
import com.example.memo.jpa.entity.company.DcContributionItem;

@Repository
public interface DcContributionItemRepository extends JpaRepository<DcContributionItem, Long> {

    @Query("""
        SELECT
            new com.example.memo.company.dto.PayableItemDto(
                i.id,
                b.status,
                m.name,
                a.accountNo,
                b.planDate,
                i.amount
            )
        FROM DcContributionItem i
        JOIN i.batch b
        JOIN i.dcMember m
        LEFT JOIN DcAccount a ON a.dcMember = m
        WHERE b.company.id = :companyId
        AND (:statuses IS NULL OR b.status IN :statuses)
        AND b.planDate BETWEEN :fromDate AND :toDate
        ORDER BY b.planDate DESC, m.name ASC
    """)
    Page<PayableItemDto> findPayableItems(
        @Param("companyId") Long companyId,
        @Param("statuses") List<DcContributionBatch.BatchStatus> statuses,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        Pageable pageable
    );
}
