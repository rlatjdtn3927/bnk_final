package com.example.memo.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RegisterViewController {

    @GetMapping("/register")
    public String showRegisterForm() {

        return "authlogin/register-form";
    }
}
