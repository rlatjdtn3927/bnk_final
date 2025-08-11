package com.example.memo.irp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/irp/join")
@RequiredArgsConstructor
public class IrpJoinViewController {
	
	@GetMapping("/step1-purpose")
	public String purposePage() {
	    return "irp/step1-purpose"; // templates/irp/step1-purpose.html
	}
	
	@GetMapping("/step2-agree")
	public String agreePage() {
	    return "irp/step2-agree"; 
	}
	
	@GetMapping("/step3-qual")
	public String qualPage() {
	    return "irp/step3-contract-qual"; 
	}
	
	@GetMapping("/step3-account")
	public String accountPage() {
	    return "irp/step3-contract-account"; 
	}
}
