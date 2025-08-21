// src/main/java/com/example/memo/purchase/controller/PageController.java
package com.example.memo.purchase.trade_common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import com.example.memo.purchase.trade_common.dto.ReserveValueDto;

@Controller
@RequestMapping("/purchase")
public class PageViewController {

    @GetMapping("/holdings")
    public String holdings() { return "purchase/holdings"; }

    @GetMapping("/products")
    public String products() { return "purchase/products"; }

    
    
 // /purchase/trade
    @GetMapping("/trade")
    public String tradeEntry(@RequestParam(name = "flow", required = false) String flow,
                             HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) {
            return "redirect:/login-view/main";
        }

        // ★ 세션에서 DTO를 꺼내고 없으면 생성
        ReserveValueDto dto = (ReserveValueDto) session.getAttribute("ReserveValueDto");
        if (dto == null) dto = new ReserveValueDto();
        dto.setFlow(flow);

        session.setAttribute("ReserveValueDto", dto); // ★ 세션에 저장
        return "redirect:/purchase/trade/step1";
    }
}
