package com.example.memo.jpa.repository.couple;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.couple.IrpSpouseLink;

import jakarta.persistence.LockModeType;

@Repository
public interface IrpSpouseLinkRepository extends JpaRepository<IrpSpouseLink, Long> {

    // 활성(신청/대기/연동) 상태 중 본인이 관련된 링크가 있는지 체크
    @Query(
      value = """
        SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
          FROM irp_spouse_link l
         WHERE l.link_status IN ('APPLIED','PENDING_ADMIN','PENDING_SPOUSE','LINKED')
           AND (l.applicant_user_id = :userId OR l.spouse_user_id = :userId)
        """,
      nativeQuery = true
    )
    int existsActiveForUserNative(@Param("userId") Long userId);

    default boolean existsActiveForUser(Long userId) {
        return existsActiveForUserNative(userId) > 0;
    }

    // 경합 방지용 잠금 조회 (필요 시 사용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM IrpSpouseLink l WHERE l.id = :id")
    Optional<IrpSpouseLink> findByIdForUpdate(@Param("id") Long id);
}