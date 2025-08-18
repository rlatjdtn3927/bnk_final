// src/main/java/com/example/memo/purchase/controller/PageController.java
package com.example.memo.purchase.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/purchase")
public class PageController {

    @GetMapping("/holdings")
    public String holdings() { return "purchase/holdings"; }

    @GetMapping("/products")
    public String products() { return "purchase/products"; }

    /**
     * 엔트리: /purchase/trade?flow=PENDING|CHANGE|MATURITY
     * - flow 미전달 시 기본 PENDING
     * - 바로 STEP1로 리다이렉트
     */
    @GetMapping("/trade")
    public String tradeEntry(@RequestParam(name = "flow", required = false) String flow) {
        String f = (flow == null || flow.isBlank()) ? "PENDING" : flow.toUpperCase();
        return "redirect:/purchase/trade/" + f + "/step1";
    }

    // 매수예정상품 변경내역
    @GetMapping("/trade/history")
    public String tradeHistory() { return "purchase/trade_history"; }
}
