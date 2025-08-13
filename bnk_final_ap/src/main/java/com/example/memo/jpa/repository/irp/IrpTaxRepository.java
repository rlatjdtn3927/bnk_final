package com.example.memo.jpa.repository.irp;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.irp.IrpTaxPurpose;

public interface IrpTaxRepository extends JpaRepository<IrpTaxPurpose, Long> {

}
