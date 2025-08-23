package com.example.memo.irp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irp/join")
@RequiredArgsConstructor
public class IrpJoinViewController {
	
	private final TcpClientService tcpService;
	private final ObjectMapper mapper;
	
	//IRP가입하러 가는 페이지
	@GetMapping("/irp-join")
	public String firstIrpPage() {
		return "irp/irp-join";
	}
	
	// 가드: 버튼 클릭 시 이 엔드포인트로 오게 한다
    @GetMapping("/start")
    public String start(HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("user");
        if (userId == null) return "redirect:/authlogin/login-view";

        // ACTIVE 여부만 차단
        ObjectNode node = mapper.createObjectNode().put("userId", userId);
        JsonNode res = tcpService.sendMessage(new TcpMessage(Command.IRP_JOIN_CHECK_ELIGIBLE, node));
        boolean eligible = res != null && res.path("eligible").asBoolean(true);

        if (!eligible) {
            ra.addFlashAttribute("toast", "이미 IRP에 가입하셨습니다");
            // 시작 페이지로 되돌리면서 flash 메시지 전달
            return "redirect:/irp/join/irp-join";
        }
        // 진행 가능하면 1단계로
        return "redirect:/irp/join/step1-purpose";
    }
	
	@GetMapping("/step1-purpose")	//(방어적 코딩)
	public String purposePage(Model model, HttpSession session, RedirectAttributes ra) {
		Long userId = (Long) session.getAttribute("user"); 
	    if (userId == null) {
	        // 로그인 안 되어 있으면 로그인 페이지로 리다이렉트
	        return "redirect:/authlogin/login-view";
	    }
	    
	    // 1) AP에 가입 가능 여부 질의 (ACTIVE면 eligible=false)
        ObjectNode node = mapper.createObjectNode().put("userId", userId);
        JsonNode res = tcpService.sendMessage(new TcpMessage(Command.IRP_JOIN_CHECK_ELIGIBLE, node));
        
        boolean eligible = res != null && res.path("eligible").asBoolean(true);
        if (!eligible) {
            ra.addFlashAttribute("toast", "이미 IRP에 가입하셨습니다");
            return "redirect:/";
        }
		model.addAttribute("userId", userId); 
		return "irp/step1-purpose"; 
	}
	
	@GetMapping("/{joinId}/tax-form")
    public String taxFormPage(@PathVariable("joinId") Long joinId, Model model) {
        model.addAttribute("joinId", joinId);
        return "irp/tax-form";	// templates/irp/tax-form.html
    }
	
	@GetMapping("/{joinId}/retire-form")
	public String retireFormPage(@PathVariable("joinId") Long joinId, Model model) {
		model.addAttribute("joinId", joinId);
		return "irp/retire-form";
	}
	
	@GetMapping("/{joinId}/step2-agree")
	public String agreePage(@PathVariable("joinId") Long joinId, Model model) {
		model.addAttribute("joinId", joinId);
	    return "irp/step2-agree";
	}
	/* /irp/join/{joinId}/step3-contract */
	@GetMapping("/{joinId}/step3-contract")
	public String contractPage(@PathVariable("joinId") Long joinId, Model model) {
		model.addAttribute("joinId", joinId);
	    return "irp/step3-contract"; 
	}
	
	@GetMapping("/{joinId}/step4-infoConfir")
	public String infoConfirPage(@PathVariable("joinId") Long joinId, Model model) {
		model.addAttribute("joinId", joinId);
		return "irp/step4-infoConfir";
	}
	
	@GetMapping("/{joinId}/step5-complete")
	public String completePage(@PathVariable("joinId") Long joinId, Model model) {
		model.addAttribute("joinId", joinId);
		return "irp/step5-complete";
	}
}
