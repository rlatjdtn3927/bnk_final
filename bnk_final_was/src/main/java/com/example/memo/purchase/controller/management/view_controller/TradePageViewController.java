// src/main/java/com/example/memo/purchase/controller/TradePageController.java
package com.example.memo.purchase.controller.management.view_controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.memo.purchase.dto.trade.PassValueDto;
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
    		model.addAttribute("PassValueDto", dto);
        return "purchase/trade/step3"; // 상품선택목록 view
    }
    /** Step 3 (상품 선택) */
    @GetMapping("/step3")
    public String step3Page(HttpSession session) {
        PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
        if (dto == null) dto = new PassValueDto();
        session.setAttribute("PassValueDto", dto);
        return "purchase/trade/step3";
    }

    @PostMapping("/step3/next")
    public String step3Next(PassValueDto dto, HttpSession session) {
        session.setAttribute("PassValueDto", dto);
        return "redirect:/purchase/trade/step4";  // ★ POST 후 GET 으로 redirect
    }

    /** Step 4 (서류 확인) */
    @GetMapping("/step4")
    public String step4Page(HttpSession session, Model model) {
        PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
        if (dto == null) {
            return "redirect:/purchase/trade/step3"; // 세션 없으면 다시 step3
        }
        model.addAttribute("PassValueDto", dto);
        return "purchase/trade/step4"; // step4.html
    }

    @PostMapping("/step4/next")
    public String step4Next(@ModelAttribute PassValueDto incoming, HttpSession session) {
        PassValueDto base = (PassValueDto) session.getAttribute("PassValueDto");
        if (base == null) base = new PassValueDto();

        // null 아닌 값만 병합(폼이 빈값일 수 있으므로)
        if (incoming.getFlow() != null)                base.setFlow(incoming.getFlow());
        if (incoming.getAccountId() != null)           base.setAccountId(incoming.getAccountId());
        if (incoming.getAccountType() != null)         base.setAccountType(incoming.getAccountType());

        if (incoming.getRiskGrade() != null)           base.setRiskGrade(incoming.getRiskGrade());
        if (incoming.getRiskGradeNum() != null)        base.setRiskGradeNum(incoming.getRiskGradeNum());
        if (incoming.getTargetProdIdList() != null)    base.setTargetProdIdList(incoming.getTargetProdIdList());
        if (incoming.getTargetProdId() != null)        base.setTargetProdId(incoming.getTargetProdId());

        if (incoming.getSourceProdIdList() != null)    base.setSourceProdIdList(incoming.getSourceProdIdList());
        if (incoming.getSourceProdId() != null)        base.setSourceProdId(incoming.getSourceProdId());
        if (incoming.getFileUrlList() != null)         base.setFileUrlList(incoming.getFileUrlList());

        session.setAttribute("PassValueDto", base);
        return "redirect:/purchase/trade/step5";
    }

    /** Step5 화면 진입 (세션만 읽음) */
    @GetMapping("/step5")
    public String step5Page(HttpSession session) {
        PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
        if (dto == null) {
            dto = new PassValueDto();
            session.setAttribute("PassValueDto", dto);
        }
        return "purchase/trade/step5";
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
