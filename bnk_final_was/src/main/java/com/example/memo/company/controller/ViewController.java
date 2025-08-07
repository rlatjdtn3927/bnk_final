package com.example.memo.company.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class ViewController {

	@GetMapping("/")
	public String root() {
		return "index";
	}
	
	@GetMapping("/company/login")
	public String loginPage() {
		return "company/login";
	}
	
	@GetMapping("/company/add-member")
	public String addMemberPage() {
		return "company/add-member";
	}
	
	@GetMapping("/company")
	public String companyMainPage(HttpServletRequest request) {
	    HttpSession session = request.getSession(false);
	    if (session == null) {
	        System.out.println("세션 없음");
	    } else {
	        Object loginManager = session.getAttribute("loginManager");
	        System.out.println("세션 로그인 정보: " + loginManager);
	    }

	    return "company/companyMain";
	}
	
	
}
