package com.example.memo.test;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<UserEntity, Integer>{
	public UserEntity findByUserId(Integer userId);
	public List<UserEntity> findAll();
}
