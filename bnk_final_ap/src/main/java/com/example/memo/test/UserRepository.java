package com.example.memo.test;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Integer>{
	public UserEntity findByUserId(Integer userId);
	public List<UserEntity> findAll();
}
