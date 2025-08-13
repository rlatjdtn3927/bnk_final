package com.example.memo.jpa.repository.irp;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.irp.TestUserEntity;

public interface TestUserRepository extends JpaRepository<TestUserEntity, Long>{
	Optional<TestUserEntity> findByUserId(Long userId);
}
