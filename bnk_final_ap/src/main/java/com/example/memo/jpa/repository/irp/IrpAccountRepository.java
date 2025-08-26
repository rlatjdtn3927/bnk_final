package com.example.memo.jpa.repository.irp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.memo.jpa.entity.irp.IrpAccount;

import jakarta.persistence.LockModeType;

public interface IrpAccountRepository extends JpaRepository<IrpAccount, String>{
	/** IRP 계좌번호로 조회 */
    Optional<IrpAccount> findByIrpAcctNo(String irpAcctNo);

    /** (선택) 사용자ID로 대표 IRP 계좌 찾기 */
    Optional<IrpAccount> findByUser_UserId(Long userId);

    Optional<IrpAccount> findTopByUserUserIdAndStatusNot(Long userId, String status);

    // (일반적) IrpAccount.user.userId 관계가 있을 때
    /** userId로 IRP 계좌번호 1건 조회 */
    @Query("""
           select a.irpAcctNo
           from IrpAccount a
           where a.user.userId = :userId
           """)
    String findIrpAcctNoByUserId(@Param("userId") Long userId);
    
    /**
     * 전체 IRP 계좌 balance 합계
     */
    @Query("select coalesce(sum(a.balance),0) from IrpAccount a")
    BigDecimal sumAllBalance();

    /**
     * 모든 IRP 계좌 번호 조회
     */
    @Query("select a.irpAcctNo from IrpAccount a")
    List<String> findAllAcctNos();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from IrpAccount a where a.irpAcctNo = :acctNo")
    Optional<IrpAccount> findForUpdate(@Param("acctNo") String acctNo);
    
    // 완료(ACTIVE) 계좌 존재 여부
    boolean existsByUser_UserIdAndStatus(Long userId, String status);
}
