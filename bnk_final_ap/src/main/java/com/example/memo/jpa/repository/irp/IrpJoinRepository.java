package com.example.memo.jpa.repository.irp;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.memo.jpa.entity.irp.IrpJoinEntity;

public interface IrpJoinRepository extends JpaRepository<IrpJoinEntity, Long>{
	
	@Query("""
	        select j from IrpJoinEntity j
	        left join fetch j.userId u
	        left join fetch j.productId p
	        left join fetch j.acctNo a
	        where j.joinId = :joinId
	""")
	Optional<IrpJoinEntity> findDetailById(@Param("joinId") Long joinId);
}
