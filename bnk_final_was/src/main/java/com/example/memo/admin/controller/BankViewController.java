package com.example.memo.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class BankViewController {
	
	@GetMapping("/login")
	public String loginPage() {
		return "admin/admin-login"; 
	}

    @GetMapping
    public String mainPage() {
        return "admin/admin-main"; 
    }
}