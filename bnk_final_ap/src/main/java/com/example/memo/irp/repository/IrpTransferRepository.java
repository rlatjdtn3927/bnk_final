package com.example.memo.irp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.irp.entity.IrpTransferPurpose;

public interface IrpTransferRepository extends JpaRepository<IrpTransferPurpose, Long> {

}
