package com.example.memo.irp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.irp.entity.TestUserEntity;

public interface TestUserRepository extends JpaRepository<TestUserEntity, Long>{
	TestUserEntity findByUserId(Long userId);
}
