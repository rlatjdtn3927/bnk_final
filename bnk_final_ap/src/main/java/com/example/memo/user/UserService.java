package com.example.memo.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.UserEntity;
import com.example.memo.jpa.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ObjectMapper mapper;
	
	public String findUser(Integer userId) throws JsonProcessingException {
		
		return mapper.writeValueAsString(userRepository.findByUserId(userId));
	}
}
