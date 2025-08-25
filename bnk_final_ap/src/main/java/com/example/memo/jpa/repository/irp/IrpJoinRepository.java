package com.example.memo.jpa.repository.irp;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.memo.admin.projection.MonthlyCountView;
import com.example.memo.jpa.entity.irp.IrpJoinEntity;

public interface IrpJoinRepository extends JpaRepository<IrpJoinEntity, Long>{
	
	@Query("""
	        select j from IrpJoinEntity j
	        left join fetch j.user u
	        left join fetch j.acctNo a
	        where j.joinId = :joinId
	""")
	Optional<IrpJoinEntity> findDetailById(@Param("joinId") Long joinId);
	
	/** 진행중(DRAFT페이지, PENDING) 중 최신 1건 */
	Optional<IrpJoinEntity> findTopByUser_UserIdAndStatusInOrderByRegDateDesc(
            Long userId, Collection<String> statuses);
	
	
	// ======= 통계 부분 : YYYY-MM로 묶어서 월별 집계 =======
    @Query(
      value = """
        SELECT TO_CHAR(reg_date, 'YYYY-MM') AS ym, COUNT(*) AS cnt
          FROM irp_join
         WHERE reg_date >= TO_DATE(:fromYm || '-01','YYYY-MM-DD')
           AND reg_date <  ADD_MONTHS(TO_DATE(:toYm || '-01','YYYY-MM-DD'), 1)
         GROUP BY TO_CHAR(reg_date, 'YYYY-MM')
         ORDER BY ym
      """,
      nativeQuery = true
    )
    List<MonthlyCountView> countMonthly(@Param("fromYm") String fromYm,
                                        @Param("toYm") String toYm);
}
