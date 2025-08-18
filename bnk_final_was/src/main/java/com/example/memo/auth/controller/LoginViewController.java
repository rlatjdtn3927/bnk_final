package com.example.memo.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/login-view") // ← 접두사 유지
public class LoginViewController {

    // 로그인 화면: GET /login-view 또는 /login-view/
    @GetMapping({"", "/"})
    public String loginPage() {
        return "authlogin/login-view";
    }


    // 메인 화면: GET /login-view/main  (세션 가드)
    @GetMapping("/main")
    public String mainPage(HttpSession session) {
        Object uid = session.getAttribute("user");
        if (uid == null) uid = session.getAttribute("LOGIN_USER_ID");
        if (uid == null) return "redirect:/login-view";
        return "authlogin/main"; 
    }

    // 로그아웃: GET /login-view/logout-view
    @GetMapping("/logout-view")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login-view";
    }
}
