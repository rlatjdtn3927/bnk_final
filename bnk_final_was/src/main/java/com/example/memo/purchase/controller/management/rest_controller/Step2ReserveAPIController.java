package com.example.memo.purchase.controller.management.rest_controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.memo.purchase.dto.util.FundRetainDto;
import com.example.memo.purchase.dto.util.UserInfoDto;

@RestController
@RequestMapping("/retain-api/reserve")
public class Step2ReserveAPIController {
	
	@PostMapping("/user-info")
	public ResponseEntity<?> userInfo() {
		UserInfoDto dto = UserInfoDto.builder()
				.accountType("IRP")
				.accountNumber("1234-1234-1234")
				.userName("김성수")
				.riskGrade("적극투자형")
				.build();
		
		return ResponseEntity.status(HttpStatus.OK).body(Map.of("userInfo", dto));
	}
	
	@PostMapping("/retain")
	public ResponseEntity<?> retain() {
		List<FundRetainDto> list = new ArrayList<>();
		
		list.add(FundRetainDto.builder()
				.productId("1")
				.productName("빨리 끝내고 싶다.")
				.category("주식형")
				.riskGradeText("매우높다")
				.retainRatio(30)
				.build());
		list.add(FundRetainDto.builder()
				.productId("2")
				.productName("빨리 끝내고 싶다.")
				.category("주식형")
				.riskGradeText("매우높다")
				.retainRatio(20)
				.build());
		list.add(FundRetainDto.builder()
				.productId("3")
				.productName("빨리 끝내고 싶다.")
				.category("주식형")
				.riskGradeText("매우높다")
				.retainRatio(50)
				.build());
		
 		return ResponseEntity.status(HttpStatus.OK).body(list);
	}
	
}
