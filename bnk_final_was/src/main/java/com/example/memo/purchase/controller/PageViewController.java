// src/main/java/com/example/memo/purchase/controller/PageController.java
package com.example.memo.purchase.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.memo.purchase.dto.trade.ReserveValueDto;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/purchase")
public class PageViewController {

    @GetMapping("/holdings")
    public String holdings() { return "purchase/holdings"; }

    @GetMapping("/products")
    public String products() { return "purchase/products"; }

    
    
    /******** 시작 ***********/	
    @GetMapping("/trade")
    public String tradeEntry(@RequestParam(name = "flow", required = false) String flow, RedirectAttributes rttr
    							,HttpSession session) {
    	Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if(userId == null) {
        	return "redirect:/login-view/main";
        }
    	ReserveValueDto dto = new ReserveValueDto();
    	dto.setFlow(flow);
    	rttr.addFlashAttribute("ReserveValueDto", dto);
        return "redirect:/purchase/trade/step1";
    }
    
}
