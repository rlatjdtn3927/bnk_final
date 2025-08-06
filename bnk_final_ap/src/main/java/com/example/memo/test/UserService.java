package com.example.memo.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.memo.tcp_common.TcpServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {
	
	@Autowired
	private UserRepository userRepository;
	
	
	public Object findUser(Integer userId) throws JsonProcessingException {
		return userRepository.findByUserId(userId);
	}
}
