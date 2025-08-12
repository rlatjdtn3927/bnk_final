package com.example.memo.jpa.repository.company;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.company.DcContributionBatch;

@Repository
public interface DcContributionBatchRepository extends JpaRepository<DcContributionBatch, Long> {

    @Query("SELECT DISTINCT b FROM DcContributionBatch b " +
           "LEFT JOIN FETCH b.company " +
           "LEFT JOIN FETCH b.uploader " +
           "LEFT JOIN FETCH b.items i " +
           "LEFT JOIN FETCH i.dcMember " +
           "WHERE b.id = :id")
    Optional<DcContributionBatch> findBatchWithItemsById(@Param("id") Long id);


    @Query(
		  value = """
		    SELECT b FROM DcContributionBatch b
		     WHERE b.company.id = :companyId
		       AND (:fromDate IS NULL OR b.planDate >= :fromDate)
		       AND (:toDate   IS NULL OR b.planDate <= :toDate)
		       AND (:status   IS NULL OR b.status = :status)
		       AND (:keyword  IS NULL OR LOWER(b.fileName) LIKE LOWER(CONCAT('%', :keyword, '%')))
		     ORDER BY b.planDate DESC, b.id DESC
		  """,
            countQuery = """
                SELECT COUNT(b) FROM DcContributionBatch b
                 WHERE b.company.id = :companyId
                   AND (:fromDate IS NULL OR b.planDate >= :fromDate)
                   AND (:toDate   IS NULL OR b.planDate <= :toDate)
                   AND (:status   IS NULL OR b.status = :status)
                   AND (:keyword  IS NULL OR LOWER(b.fileName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """
        )
        Page<DcContributionBatch> findList(
            @Param("companyId") Long companyId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") DcContributionBatch.BatchStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
        );

}