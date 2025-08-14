package com.example.memo.purchase.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/purchase")
public class PageController {
	//보유 현황 페이지
    @GetMapping("/holdings")
    public String holdings() {
        return "purchase/holdings";
    }
    //상품 관리 페이지
    @GetMapping("/products")
    public String products() {
        return "purchase/products";
    }
}
