// src/main/java/com/example/memo/purchase/controller/PageController.java
package com.example.memo.purchase.trade_common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.memo.purchase.trade_common.dto.ChangeValueDto;
import com.example.memo.purchase.trade_common.dto.ReserveValueDto;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/purchase")
public class PageViewController {

    @GetMapping("/holdings")
    public String holdings() { return "purchase/holdings"; }

    @GetMapping("/products")
    public String products() { return "purchase/products"; }

    
    
    //매수예정상품
    @GetMapping("/trade/reserve")
    public String tradeReserveEntry(@RequestParam(name = "flow", required = false) String flow,
                             HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");


        // 세션 DTO 준비
        ReserveValueDto dto = (ReserveValueDto) session.getAttribute("ReserveValueDto");
        if (dto == null) dto = new ReserveValueDto();
        dto.setFlow(flow);
        session.setAttribute("ReserveValueDto", dto);

        // ✅ 반드시 step1 로 리다이렉트 (step1 컨트롤러가 AP 호출/세션 세팅)
        return "redirect:/purchase/trade/reserve/step1";
    }
    
    //보유상품변경
    @GetMapping("/trade/change")
    public String tradeChangeEntry(@RequestParam(name = "flow", required = false) String flow,
                             HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");


        // 세션 DTO 준비
        ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
        if (dto == null) dto = new ChangeValueDto();
        dto.setFlow(flow);
        session.setAttribute("ChangeValueDto", dto);

        // ✅ 반드시 step1 로 리다이렉트 (step1 컨트롤러가 AP 호출/세션 세팅)
        return "redirect:/purchase/trade/change/step1";
    }
}
