package com.example.memo.jpa.repository.couple;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.couple.IrpSpouseLink;
import com.example.memo.jpa.entity.couple.LinkStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface IrpSpouseLinkRepository extends JpaRepository<IrpSpouseLink, Long> {

    /* ================= 기존 ================= */

    // (기존 existsActiveForUser* 는 아래 2)로 대체 권장)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM IrpSpouseLink l WHERE l.id = :id")
    Optional<IrpSpouseLink> findByIdForUpdate(@Param("id") Long id);

    List<IrpSpouseLink> findByLinkStatusOrderByAppliedAtDesc(LinkStatus status);


    /* ================= 추가/수정 ================= */

    // 1) '열린(open)' 상태 집합
    //    APPLIED, OCR_FAILED(쓰고 있다면), PENDING_ADMIN, PENDING_SPOUSE 만 포함
    //    - LINKED(완료), REJECTED_* 등 종결 상태는 제외
    default List<LinkStatus> openStatuses() {
        return List.of(
            LinkStatus.APPLIED,
            LinkStatus.PENDING_ADMIN,
            LinkStatus.PENDING_SPOUSE
            // 필요시 OCR_FAILED 상태 쓰면 여기에 추가
        );
    }

    // 2) 신청자 기준, 열린 상태의 '최신 1건' (id desc)
    @Query("""
        select l from IrpSpouseLink l
        where l.applicantUserId = :applicantUserId
          and l.linkStatus in :statuses
        order by l.id desc
    """)
    List<IrpSpouseLink> findOpenByApplicantOrderByIdDesc(
            @Param("applicantUserId") Long applicantUserId,
            @Param("statuses") List<LinkStatus> statuses
    );

    // 편의 default: Optional로 최신 1건만 리턴
    default Optional<IrpSpouseLink> findLatestOpenByApplicant(Long applicantUserId) {
        List<IrpSpouseLink> rows = findOpenByApplicantOrderByIdDesc(applicantUserId, openStatuses());
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    // 3) '진짜 활성(완료된 연동)' 존재 여부만 체크 (신규 생성 차단 용)
    @Query(
      value = """
        SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
          FROM irp_spouse_link l
         WHERE l.link_status = 'LINKED'
           AND (l.applicant_user_id = :userId OR l.spouse_user_id = :userId)
        """,
      nativeQuery = true
    )
    int existsLinkedForUserNative(@Param("userId") Long userId);

    default boolean existsLinkedForUser(Long userId) {
        return existsLinkedForUserNative(userId) > 0;
    }
    
    // 사용자(신청자 or 배우자) 기준 최신 1건
    @Query("""
        select l from IrpSpouseLink l
         where l.applicantUserId = :userId or l.spouseUserId = :userId
         order by l.id desc
    """)
    List<IrpSpouseLink> findLatestByUser(@Param("userId") Long userId);

    default Optional<IrpSpouseLink> findLatestOneByUser(Long userId) {
        var rows = findLatestByUser(userId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
