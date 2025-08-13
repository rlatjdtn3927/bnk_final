package com.example.memo.irp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.memo.irp.entity.IrpJoinEntity;

public interface IrpJoinRepository extends JpaRepository<IrpJoinEntity, Long>{
	
	@Query("""
	        select j from IrpJoinEntity j
	        join fetch j.userId u
	        join fetch j.productId p
	        join fetch j.acctNo a
	        where j.joinId = :joinId
	""")
	Optional<IrpJoinEntity> findDetailById(@Param("joinId") Long joinId);
}
