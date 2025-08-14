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
	
	@GetMapping("/irp-join")
	public String firstIrpPage() {
		return "irp/irp-join";
	}
	
	@GetMapping("/{joinId}/tax-form")
    public String taxFormPage(@PathVariable Long joinId, Model model) {
        model.addAttribute("joinId", joinId);
        return "irp/tax-form";
    }
	
	@GetMapping("/retire-form")
	public String retireFormPage() {
		return "irp/retire-form";
	}
	
	@GetMapping("/step1-purpose")
	public String purposePage() {
	    return "irp/step1-purpose"; // templates/irp/step1-purpose.html
	}
	/*
	@GetMapping("/step1-qual")
	public String qualPage() {
		return "irp/step1-contract-qual"; 
	}
	*/
	@GetMapping("/step2-agree")
	public String agreePage() {
	    return "irp/step2-agree"; 
	}
	
	@GetMapping("/step3-account")
	public String accountPage() {
	    return "irp/step3-contract-account"; 
	}
}
