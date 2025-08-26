package com.example.memo.jpa.repository.rule;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.memo.jpa.entity.rule.ProfileHistory;

public interface ProfileHistoryRepository extends JpaRepository<ProfileHistory, Long> {

    /** 최신 1건(analyzedAt DESC)에서 type_name, type_id만 추출 */
    @Query("""
           select t.typeName, t.typeId
           from ProfileHistory h
           join h.type t
           where h.userId = :userId
           order by h.analyzedAt desc
           """)
    List<Object[]> findLatestTypeNameAndIdByUserId(@Param("userId")Long userId, Pageable pageable);

    /* analyzedAt이 없다면 증가형 PK 역정렬 대안
    @Query("""
           select t.typeName, t.typeId
           from ProfileHistory h join h.type t
           where h.userId = :userId
           order by h.historyId desc
           """)
    List<Object[]> findLatestTypeNameAndIdByUserIdOrderByPk(Long userId, Pageable pageable);
    */
}
