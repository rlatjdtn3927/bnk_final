package com.example.memo.irp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.irp.entity.TestUserEntity;

public interface TestUserRepository extends JpaRepository<TestUserEntity, Long>{
	Optional<TestUserEntity> findByUserId(Long userId);
}
