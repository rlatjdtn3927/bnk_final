package com.example.memo.purchase.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/purchase")
public class PageController {

    @GetMapping("/holdings")
    public String holdings() { return "purchase/holdings"; }

    @GetMapping("/products")
    public String products() { return "purchase/products"; }

    // 공통 위저드 (등록/변경/만기): /purchase/trade?flow=PENDING|CHANGE|MATURITY
    @GetMapping("/trade")
    public String tradeWizard() { return "purchase/trade_wizard"; }

    // 매수예정상품 변경내역
    @GetMapping("/trade/history")
    public String tradeHistory() { return "purchase/trade_history"; }
}
