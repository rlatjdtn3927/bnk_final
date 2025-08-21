package com.example.memo.purchase.reserve.rest_controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.purchase.reserve.dto.RetainRequestDto;
import com.example.memo.purchase.reserve.dto.UserInfoDto;
import com.example.memo.purchase.trade_common.dto.PassValueDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/retain-api/reserve")
@RequiredArgsConstructor
public class ReserveRetainAPIController {
	
	private final TcpClientService tcpClientService;
	private final ObjectMapper mapper;
	
    /** 상단 사용자/계좌 카드용 데이터 */
	@PostMapping("/user-info")
	public ResponseEntity<?> userInfo(HttpSession session) {
	    PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
	    String userName = (String) session.getAttribute("userName");

	    // 세션에 값이 없을 때도 NPE 없이 안전하게
	    if (dto == null) {
	        UserInfoDto empty = UserInfoDto.builder()
	            .accountId("-")
	            .accountType("-")
	            .userName(userName != null ? userName : "-")
	            .riskGrade("-")
	            .riskGradeNum(0)
	            .build();
	        return ResponseEntity.ok(Map.of("userInfo", empty));
	    }

	    String riskText = dto.getRiskGrade();
	    int riskGradeNum;
	    switch (riskText != null ? riskText : "") {
	        case "안정형":   riskGradeNum = 5; break;
	        case "안전추구형": riskGradeNum = 4; break;
	        case "위험중립형": riskGradeNum = 3; break;
	        case "적극투자형": riskGradeNum = 2; break;
	        case "공격투자형": riskGradeNum = 1; break;
	        default:         riskGradeNum = 0; break; // 미정/없음
	    }

	    dto.setRiskGradeNum(riskGradeNum);
	    session.setAttribute("PassValueDto", dto);

	    UserInfoDto userInfo = UserInfoDto.builder()
	        .accountId(dto.getAccountId())
	        .accountType(dto.getAccountType())
	        .userName(userName != null ? userName : "-")
	        .riskGrade(riskText != null ? riskText : "-")
	        .riskGradeNum(riskGradeNum) // ★ 포함!
	        .build();

	    return ResponseEntity.ok(Map.of("userInfo", userInfo));
	}

    /** 보유/이미 선택된 상품 목록(운용비율 포함) */
	@PostMapping("/retain")
    public ResponseEntity<?> retainList(HttpSession session) {
    	
    	RetainRequestDto dto = new RetainRequestDto();
    	dto.setUserId((Long)session.getAttribute("userId"));
    	JsonNode msg = mapper.valueToTree(dto);
    	TcpMessage message = new TcpMessage(Command.BUY_PLAN_CURRENT, msg);
    	JsonNode response = tcpClientService.sendMessage(message);
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

	
}


//@PostMapping("/user-info")
//public ResponseEntity<?> userInfo() {
//	Integer riskGradeNum = 0;
//	switch (riskText) {
//		case "안정형" : {
//			riskGradeNum = 2;
//			break;
//		}
//		case "안전추구형": {
//			riskGradeNum = 3;
//			break;
//		}
//		case "위험중립형": {
//			riskGradeNum = 4;
//			break;
//		}
//		case "적극투자형": {
//			riskGradeNum = 5;
//			break;
//		}
//		case "공격투자형" : {
//			riskGradeNum = 6;
//			break;
//		}	
//	}
//	UserInfoDto dto = UserInfoDto.builder()
//			.accountType("IRP")
//			.accountNumber("1234-1234-1234")
//			.userName("김성수")
//			.riskGrade(riskText)
//			.riskGradeNum(riskGradeNum)
//			.build();
//	
//	return ResponseEntity.status(HttpStatus.OK).body(Map.of("userInfo", dto));
//}

//@PostMapping("/retain")
//public ResponseEntity<?> retain() {
//	List<FundRetainDto> list = new ArrayList<>();
//	
//	list.add(FundRetainDto.builder()
//			.productId("1")
//			.productName("빨리 끝내고 싶다.")
//			.category("주식형")
//			.riskGradeText("매우높다")
//			.retainRatio(30)
//			.build());
//	list.add(FundRetainDto.builder()
//			.productId("2")
//			.productName("빨리 끝내고 싶다.")
//			.category("주식형")
//			.riskGradeText("매우높다")
//			.retainRatio(20)
//			.build());
//	list.add(FundRetainDto.builder()
//			.productId("3")
//			.productName("빨리 끝내고 싶다.")
//			.category("주식형")
//			.riskGradeText("매우높다")
//			.retainRatio(50)
//			.build());
//	
//		return ResponseEntity.status(HttpStatus.OK).body(list);
//}
