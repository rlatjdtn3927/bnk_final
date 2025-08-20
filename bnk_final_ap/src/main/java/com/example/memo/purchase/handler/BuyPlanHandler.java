package com.example.memo.purchase.handler;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.dto.trade.CurrentRetainDto;
import com.example.memo.purchase.service.BuyPlanService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BuyPlanHandler implements TcpMessageHandler{
	
	private final BuyPlanService buyPlanService;

	@Override
	public boolean supports(Command command) {
		return command.name().startsWith("BUY_PLAN_");
	}

	@Override
	public Object handle(Command command, JsonNode data) {
		switch(command.name()) {
			case "BUY_PLAN_CURRENT": {
				List<CurrentRetainDto> result = buyPlanService.getCurrentRetain(data);
				if(result == null) {
					return Map.of("error", "보유 상품 목록 불러오기 실패.");
				} else {
					return Map.of("resultList", result);
				}
				
			}
			default: {
				return Map.of("error", "보유 상품 목록 불러오기 실패.");
			}
		}
	}
}
