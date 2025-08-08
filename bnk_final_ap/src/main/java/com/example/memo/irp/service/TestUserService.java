package com.example.memo.irp.service;

import org.springframework.stereotype.Service;

import com.example.memo.irp.entity.TestUserEntity;
import com.example.memo.irp.repository.TestUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestUserService {
	
	private final TestUserRepository userRepository;
	
	public TestUserEntity getUser(Long userId) {
        return userRepository.findByUserId(userId);
    }
}
