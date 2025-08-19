// src/main/java/com/example/memo/purchase/controller/PageController.java
package com.example.memo.purchase.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.memo.purchase.dto.ModelValueDto;

@Controller
@RequestMapping("/purchase")
public class PageViewController {

    @GetMapping("/holdings")
    public String holdings() { return "purchase/holdings"; }

    @GetMapping("/products")
    public String products() { return "purchase/products"; }

    
    
    /******** 시작 ***********/	
    @GetMapping("/trade")
    public String tradeEntry(@RequestParam(name = "flow", required = false) String flow, RedirectAttributes rttr) {
    	ModelValueDto dto = new ModelValueDto();
    	dto.setFlow(flow);
    	rttr.addAttribute("ModelValueDto", dto);
        return "redirect:/purchase/trade/step1";
    }
    
}
