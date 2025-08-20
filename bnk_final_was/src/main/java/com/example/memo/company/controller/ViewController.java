package com.example.memo.company.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class ViewController {

	@GetMapping("/")
	public String root() {
		return "index";
	}
	
	@GetMapping("/test")
	public String test() {
		return "test";
	}
	
	@GetMapping("/company/login")
	public String loginPage() {
		return "company/login";
	}
	
	@GetMapping("/company/add-member")
	public String addMemberPage() {
		return "company/add-member";
	}
	
	@GetMapping("/company/subscriber-list")
	public String subscriberListPage() {
		return "company/subscriber-list";
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

	    return "company/company-main";
	}
	
	@GetMapping("/company/dc/contribution/upload-page")
	public String contributionUploadPage() {
	    return "company/contribution-upload";
	}


	@GetMapping("/company/dc/contribution/result-page")
	public String contributionResultPage() {
	    return "company/contribution-result";
	}
	
	@GetMapping("/company/dc/contribution/plan-list-page")
    public String contributionPlanListPage() {
        return "company/contribution-plan-list";
    }
	
	@GetMapping("/company/dc/contribution/deposit-page")
	public String contributionDepositPage() {
		return "company/contribution-deposit";
	}
	
	
	@GetMapping("/company/dc/contribution/payable-list-page")
	public String contribPayListPage() {
		return "company/contribution-payable-list";
	}
	
	 @GetMapping("/company/dc/contribution/result-final")
	    public String resultFinal(
	            @RequestParam(name="batchId", required = false) Long batchId,
	            @RequestParam(name="okCount", required = false) Integer okCount,
	            @RequestParam(name="totalAmount", required = false) String totalAmount,
	            Model model) {
	        // 필요하면 모델에 담아서 Thymeleaf에서 바로 쓰기
	        model.addAttribute("batchId", batchId);
	        model.addAttribute("okCount", okCount);
	        model.addAttribute("totalAmount", totalAmount);
	        // templates/company/contribution-result-test.html
	        return "company/contribution-result-test";
	 }
}
