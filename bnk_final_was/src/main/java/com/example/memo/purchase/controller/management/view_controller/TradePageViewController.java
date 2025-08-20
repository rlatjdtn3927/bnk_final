// src/main/java/com/example/memo/purchase/controller/TradePageController.java
package com.example.memo.purchase.controller.management.view_controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.memo.purchase.dto.trade.PassValueDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/purchase/trade")
@RequiredArgsConstructor
public class TradePageViewController {
	

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @GetMapping("/step1") //model: flow
    public String step1(HttpSession session, RedirectAttributes rttr) {
    		PassValueDto dto = (PassValueDto)rttr.getAttribute("PassValueDto"); 
    		session.setAttribute("PassValueDto", dto);
        return "purchase/trade/step1"; //계좌선택 view
    }
	
	@GetMapping("/dummy")
	public String dummy() {
		return "purchase/trade/step1_form";
	}

    /** step2 화면 진입 (이전 단계에서 넘겨준 모델을 그대로 뷰에 바인딩) */
    @PostMapping("/step1/after-risk")
    public String step2After(@ModelAttribute PassValueDto dto, HttpSession session) {
    	session.setAttribute("PassValueDto", dto);
    	session.setAttribute("userId", 1L);
    	session.setAttribute("userName", "김성수");
        return "purchase/trade/step2_reserve";
    }

}

