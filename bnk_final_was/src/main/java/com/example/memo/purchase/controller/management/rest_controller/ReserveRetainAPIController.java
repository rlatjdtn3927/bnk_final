package com.example.memo.purchase.controller.management.rest_controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.memo.purchase.dto.util.FundRetainDto;
import com.example.memo.purchase.dto.util.UserInfoDto;

import ch.qos.logback.core.model.Model;

@RestController
@RequestMapping("/retain-api/reserve")
public class ReserveRetainAPIController {
	
    /** 상단 사용자/계좌 카드용 하드코딩 데이터 */
    @PostMapping("/user-info")
    public Map<String, Object> userInfo() {
        Map<String, Object> user = new HashMap<>();
        user.put("userName", "홍길동");
        user.put("accountType", "IRP");
        user.put("accountNumber", "123-45-67890");
        user.put("riskGrade", "적극투자형"); // 뷰 상단 뱃지
        user.put("riskGradeNum", 5);       // 모달 요청 바디에 들어가는 값과 매칭

        return Map.of("userInfo", user);
    }

    /** 보유/이미 선택된 상품 목록(운용비율 포함) */
    @PostMapping("/retain")
    public List<Map<String, Object>> retainList() {
        List<Map<String, Object>> list = new ArrayList<>();

        // 정기예금 예시
        list.add(new HashMap<>() {{
            put("productId", "DEP-IRP-001");
            put("productName", "SBI저축은행퇴직연금정기예금(개인형IRP) 1년제");
            put("retainRatio", 30);                // 현재 운용비율
            put("category", "정기예금");
            put("riskGradeText", "원리금보장");
        }});

        // 펀드 예시
        list.add(new HashMap<>() {{
            put("productId", "FUND-001");
            put("productName", "신한BNPP코어주식형(퇴직연금)");
            put("retainRatio", 40);
            put("category", "펀드");
            put("riskGradeText", "높음");
        }});

        // ETF 예시
        list.add(new HashMap<>() {{
            put("productId", "ETF-002");
            put("productName", "KODEX S&P500(퇴직연금)");
            put("retainRatio", 30);
            put("category", "ETF");
            put("riskGradeText", "중간");
        }});

        return list;
    }

	
}


//@PostMapping("/user-info")
//public ResponseEntity<?> userInfo(@RequestParam String riskText) {
//	String[] riskGrades = {"안정형", "안전추구형", "위험중립형", "적극투자형", "공격투자형"};
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
//		
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
