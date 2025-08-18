// src/main/java/com/example/memo/purchase/controller/TradePageController.java
package com.example.memo.purchase.controller.management.view_controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.memo.purchase.dto.ModelValueDto;

@Controller
@RequestMapping("/purchase/trade")
@RequiredArgsConstructor
public class TradePageViewController {
		
    @GetMapping("/step1") //model: flow
    public String step1(Model model, RedirectAttributes rttr) {
    		ModelValueDto dto = (ModelValueDto)rttr.getAttribute("ModelValueDto"); 
    		model.addAttribute("ModelValueDto", dto);
        return "purchase/trade/step1"; //계좌선택 view
    }

    @PostMapping("/step1/next") //model: flow, 계좌 id, type 
    public String step1Next(@RequestBody ModelValueDto dto, Model model) {
    		model.addAttribute("ModelValueDto", dto);
    		if(dto.getFlow().equals("RESERVE")) {
    			 return "purchase/trade/step2_reserve"; // 보유상품목록 view
    		} else {
    			 return "purchase/trade/step2_others"; // 보유상품목록 view
    		}
    }
    
    @PostMapping("/step2/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID
    public String step2Next(@RequestBody ModelValueDto dto, Model model) {
    		model.addAttribute("ModelValueDto", dto);
        return "purchase/trade/step3"; // 상품선택목록 view
    }
    
    @PostMapping("/step3/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
    public String step3Next(@RequestBody ModelValueDto dto, Model model) {
    		model.addAttribute("ModelValueDto", dto);
        return "purchase/trade/step4"; // 서류 view
    }
    
    @PostMapping("/step4/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
    public String step4Next(@RequestBody ModelValueDto dto, Model model) {
    		model.addAttribute("ModelValueDto", dto);
        return "purchase/trade/step5"; // 비교확인 view
    }
    
    @PostMapping("/step5/next") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
    public String step5Next() {
        return "redirect:purchase/trade/step6"; // 완료 리다이렉트
    }
    
    @GetMapping("/step6") //model: flow, 계좌 id, type, 투자성향등급 or 대상 상품 ID, 선택 상품 ID or IDs
    public String step6() {
        return "purchase/trade/step6"; // 완료 view
    }
}
