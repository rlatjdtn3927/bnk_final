package com.example.memo.user;

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
	public String handle(Command command, JsonNode data) throws JsonProcessingException {
	    switch (command) {
	        case USER_GET:
	        	return userService.findUser(data.get("userId").asInt());
	        case USER_DELETE:
	            String deleteId = data.get("userId").asText();
	            return "사용자 삭제됨 - ID: " + deleteId;
	        default:
	            return "알 수 없는 명령: " + command.name();
	    }
	}
	
}
