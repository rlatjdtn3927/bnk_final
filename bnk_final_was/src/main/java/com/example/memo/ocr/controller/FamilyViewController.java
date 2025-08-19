package com.example.memo.ocr.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FamilyViewController {
	
	@GetMapping("/family/cert")     // 브라우저에서 이 URL로 접속
    public String familyCertPage() {
        return "ocr/family-cert";
    }

}
