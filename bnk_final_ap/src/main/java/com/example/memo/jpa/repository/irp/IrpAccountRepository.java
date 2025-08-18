package com.example.memo.jpa.repository.irp;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.irp.IrpAccount;

public interface IrpAccountRepository extends JpaRepository<IrpAccount, String>{
	/** IRP 계좌번호로 조회 */
    Optional<IrpAccount> findByIrpAcctNo(String irpAcctNo);

    /** (선택) 사용자ID로 대표 IRP 계좌 찾기 */
    Optional<IrpAccount> findByUser_UserId(Long userId);
}
