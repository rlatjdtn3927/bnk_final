package com.example.memo.jpa.repository.irp;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.irp.IrpTransferPurpose;

public interface IrpTransferRepository extends JpaRepository<IrpTransferPurpose, Long> {

}
