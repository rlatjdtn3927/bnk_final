package com.example.memo.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {
  @GetMapping("/hello")
  public String hello(){ return "hello"; } // 템플릿명
}
