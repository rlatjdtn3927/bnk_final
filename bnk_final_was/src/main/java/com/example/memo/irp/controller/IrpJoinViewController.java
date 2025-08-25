package com.example.memo.irp.controller;

import java.util.ArrayList;
import java.util.List;

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
	
	@GetMapping("/deposit")
	public String irpDeposit(HttpSession session, Model model) {
	    Long loginId = (Long) session.getAttribute("LOGIN_USER_ID");
	    if (loginId == null) {
	        throw new IllegalStateException("로그인 세션이 만료되었습니다.");
	    }

	    System.out.println("== [/irp/deposit] 세션 로그인ID=" + loginId);

	    try {
	        // ✅ IRP 계좌 1건 조회
	        JsonNode Req = mapper.createObjectNode().put("userId", loginId);
	        TcpMessage irpMsg = new TcpMessage(Command.ACCOUNT_GET_IRP_BY_USER, Req);
	        JsonNode irpRes = tcpService.sendMessage(irpMsg);

	        System.out.println("== [IRP 계좌 응답] " + irpRes.toPrettyString());

	        // 루트에서 바로 irpAcctNo 추출
	        String irpAccountId = irpRes.path("irpAcctNo").asText(null);
	        System.out.println("   IRP 계좌번호: " + irpAccountId);

	        // ✅ DC 계좌 여러 건 조회
	        TcpMessage dcMsg = new TcpMessage(Command.ACCOUNT_GET_DC_LIST_BY_USER, Req);
	        JsonNode dcRes = tcpService.sendMessage(dcMsg);

	        System.out.println("== [DC 계좌 응답] " + dcRes.toPrettyString());

	        List<String> dcAccounts = new ArrayList<>();
	        // dcRes = { "dcAccounts": [ "..." ] } 형태니까 root에서 바로 꺼내기
	        if (dcRes.has("dcAccounts") && dcRes.path("dcAccounts").isArray()) {
	            for (JsonNode node : dcRes.path("dcAccounts")) {
	                dcAccounts.add(node.asText());
	            }
	        }
	        System.out.println("   DC 계좌번호들: " + dcAccounts);

	        // View 전달
	        model.addAttribute("irpAccountId", irpAccountId);
	        model.addAttribute("dcAccounts", dcAccounts);

	    } catch (Exception e) {
	        e.printStackTrace();
	        model.addAttribute("irpAccountId", null);
	        model.addAttribute("dcAccounts", List.of());
	    }

	    return "irp/deposit";
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
