package com.example.memo.purchase.reserve.handler;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.reserve.dto.response.CurrentRetainDto;
import com.example.memo.purchase.reserve.service.BuyPlanService;
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
					return Map.of("error", "보유 상품 목록 불러오기 실패.");  /*조회 실패 시에 반환하는 곳*/
				} else {
					return Map.of("resultList", result); /*조회 성공 시에 반환하는 곳*/
				}	
			}
			case "BUY_PLAN_UPDATE" : {
				String resultMsg = buyPlanService.updateBuyPlan(data);
				return Map.of("result", resultMsg); // 업데이트 결과값 반환
			}	
			default: {
				return Map.of("error", "보유 상품 목록 불러오기 실패.");
			}
		}
	}
}
