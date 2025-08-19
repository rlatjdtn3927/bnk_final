package com.example.memo.irp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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
	public String purposePage(Model model) {
		model.addAttribute("userId", 2L); //테스트용 고정 ID
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
	/*/irp/join/23/step3-contract*/
	@GetMapping("/{joinId}/step3-contract")
	public String contractPage(@PathVariable("joinId") Long joinId, Model model) {
		model.addAttribute("joinId", joinId);
	    return "irp/step3-contract"; 
	}
	
	@GetMapping("/{joinId}/step4-infoConfir")
	public String portfolioPage(@PathVariable("joinId") Long joinId, Model model) {
		model.addAttribute("joinId", joinId);
		return "irp/step4-infoConfir";
	}
}
