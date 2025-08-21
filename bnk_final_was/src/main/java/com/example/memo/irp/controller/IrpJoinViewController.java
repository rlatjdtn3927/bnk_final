package com.example.memo.irp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irp/join")
@RequiredArgsConstructor
public class IrpJoinViewController {
	
	//IRP가입하러 가는 페이지
	@GetMapping("/irp-join")
	public String firstIrpPage() {
		return "irp/irp-join";
	}
	
	@GetMapping("/step1-purpose")
	public String purposePage(Model model, HttpSession session) {
		Long userId = (Long) session.getAttribute("user"); 
	    if (userId == null) {
	        // 로그인 안 되어 있으면 로그인 페이지로 리다이렉트
	        return "redirect:/authlogin/login-view";
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
