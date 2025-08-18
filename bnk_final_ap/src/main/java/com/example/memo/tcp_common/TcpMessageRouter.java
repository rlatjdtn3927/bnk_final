package com.example.memo.tcp_common;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/*신경 안써도 되는 클래스*/

@Component
public class TcpMessageRouter {
		
	@Autowired
	private ObjectMapper mapper;
	
	private final Map<Command, TcpMessageHandler> handlerMap = new EnumMap<>(Command.class);
	
    public TcpMessageRouter(List<TcpMessageHandler> handlers) {
		for(TcpMessageHandler handler : handlers) {
			for(Command command : Command.values()) {
				if(handler.supports(command)) {
					handlerMap.put(command, handler);
				}
			}
		}
    }
	
	public String route(TcpMessage msg) throws JsonProcessingException {
		Command command = msg.getCommand();
		TcpMessageHandler handler = handlerMap.get(command);
		if(handler == null) {
			System.out.println("정의되지 않은 프로토콜입니다.");
		}
		Object result = handler.handle(command, msg.getData()); 
		return mapper.writeValueAsString(result);
	}
	
}
