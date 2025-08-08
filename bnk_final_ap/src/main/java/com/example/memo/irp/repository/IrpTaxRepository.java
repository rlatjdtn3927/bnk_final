package com.example.memo.irp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.irp.entity.IrpTaxPurpose;

public interface IrpTaxRepository extends JpaRepository<IrpTaxPurpose, Long> {

}
