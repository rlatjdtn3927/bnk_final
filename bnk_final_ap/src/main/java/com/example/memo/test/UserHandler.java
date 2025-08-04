package com.example.memo.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.memo.jpa.repository.UserRepository;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class UserHandler implements TcpMessageHandler{
	
	@Autowired
	private UserService userService;

	@Override
	public boolean supports(Command command) {
		return command.name().startsWith("USER_");
	}

	@Override
	public Object handle(Command command, JsonNode data) {
	    switch (command) {
	        case USER_GET:
			try {
				return userService.findUser(data.get("userId").asInt());
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
	        default:
	            return "알 수 없는 명령: " + command.name();
	    }
	}
	
}
