package com.example.memo.irp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.irp.TestUserEntity;
import com.example.memo.jpa.repository.irp.TestUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestUserService {
	
	private final TestUserRepository userRepository;
	
	@Transactional
	public TestUserEntity getUser(Long userId) {
        return userRepository.findByUserId(userId).orElseThrow();
    }
}
