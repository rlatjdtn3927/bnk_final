// src/main/java/com/example/memo/purchase/controller/TradePageController.java
package com.example.memo.purchase.controller.management.view_controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.memo.purchase.dto.PassValueDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/purchase/trade")
@RequiredArgsConstructor
public class TradePageViewController {
	
	private final TcpClientService tcpClientService;
    private final ObjectMapper objectMapper;
	
	@GetMapping("/dummy")
	public String dummy() {
		return "purchase/trade/step1_form";
	}
		
	@GetMapping("/step1")
	public String step1(@ModelAttribute("PassValueDto") PassValueDto dto, HttpSession session) {
	    // 세션에 사용자 정보와 flow DTO 저장
		if(session.getAttribute("userId")==null) {
			session.setAttribute("userId", 1001L);
		}
	    session.setAttribute("PassValueDto", dto);

	    return "purchase/trade/step1"; // 계좌 선택 view
	}

	@PostMapping("/step1/next")
    public String step1Next(@RequestParam("accountType") String accountType,
                            @RequestParam("accountId")   String accountId,
                            HttpSession session) {

        PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
        if (dto == null) dto = new PassValueDto();
        dto.setAccountType(accountType);
        dto.setAccountId(accountId);
        session.setAttribute("PassValueDto", dto);

        if ("RESERVE".equalsIgnoreCase(dto.getFlow())) {
            return "redirect:/survey-view";           // 투자성향 분석
        } else {
            return "purchase/trade/step2_others";     // 보유상품목록
        }
    }

 
    //@GetMapping("/step2") purchase/trade/step2_reserve.html로 가는 함수만들기
    //ap에 접속해서 profile_result.type_id를 이용해서 profile_type.type_name을 가지고 오자
    @GetMapping("/step2")
    public String step2Page(Model model) {
        try {
            Long userId = 1001L; // 테스트용 하드코딩

            ObjectNode req = objectMapper.createObjectNode()
                                         .put("userId", userId);

            TcpMessage msg = new TcpMessage(Command.PROFILE_RESULT_GET, req);
            JsonNode response = tcpClientService.sendMessage(msg);

            JsonNode data = response.path("data");
            if (data.has("data")) data = data.get("data");

            String typeName = data.path("typeName").asText(null);
            model.addAttribute("profileType", typeName);

        } catch (Exception e) {
            model.addAttribute("error", "투자성향 조회 오류: " + e.getMessage());
        }

        return "purchase/trade/step2_reserve";
    }
   
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    @PostMapping("/step2/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID
    public String step2Next(PassValueDto dto, Model model) {
    		model.addAttribute("ModelValueDto", dto);
        return "purchase/trade/step3"; // 상품선택목록 view
    }
    
    
    
    @PostMapping("/step3/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
    public String step3Next(PassValueDto dto, Model model) {
    		model.addAttribute("ModelValueDto", dto);
        return "purchase/trade/step4"; // 서류 view
    }
    
    @PostMapping("/step4/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
    public String step4Next(PassValueDto dto, Model model) {
    		model.addAttribute("ModelValueDto", dto);
        return "purchase/trade/step5"; // 비교확인 view
    }
    
    @PostMapping("/step5/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
    public String step5Next() {
        return "redirect:purchase/trade/step6"; // 완료 리다이렉트
    }
    
    @GetMapping("/step6") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
    public String step6() {
        return "purchase/trade/step6"; // 완료 view
    }
}
