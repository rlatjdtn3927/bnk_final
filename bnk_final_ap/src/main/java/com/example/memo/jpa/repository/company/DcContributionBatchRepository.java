package com.example.memo.jpa.repository.company;

import java.util.Optional;

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
}