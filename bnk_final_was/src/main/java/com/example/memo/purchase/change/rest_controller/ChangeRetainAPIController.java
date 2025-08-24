package com.example.memo.purchase.change.rest_controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.purchase.change.dto.RetainHoldingsDto;
import com.example.memo.purchase.reserve.dto.UserInfoDto;
import com.example.memo.purchase.trade_common.dto.ChangeValueDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/retain-api/change")
@RequiredArgsConstructor
public class ChangeRetainAPIController {
	private final TcpClientService tcpClientService;
	private final ObjectMapper mapper;
	
    /** 상단 사용자/계좌 카드용 데이터 */
	@PostMapping("/user-info")
	public ResponseEntity<?> userInfo(HttpSession session) {
	    ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
	    String userName = (String) session.getAttribute("username");

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
	    session.setAttribute("ChangeValueDto", dto);

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
    public ResponseEntity<?> retainList(@RequestParam("accountType") String accountType, @RequestParam("accountId") String accountId) {
    	
    	RetainHoldingsDto dto = new RetainHoldingsDto(accountType,accountId);
    	JsonNode msg = mapper.valueToTree(dto);
    	TcpMessage message = new TcpMessage(Command.CHANGE_GET_HOLDINGS, msg);
    	JsonNode response = tcpClientService.sendMessage(message);
        System.out.println(response.toString()); //여기서 받아온 데이터 형식 확인하십쇼
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
