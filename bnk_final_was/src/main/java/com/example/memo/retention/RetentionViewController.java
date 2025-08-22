package com.example.memo.retention;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RetentionViewController {
	
    @GetMapping("/retain/holdings")
    public String holdingsPage() {
        return "retention/holdings";
    }

    /** 보유 계좌 리스트 (retention-01) */
    @GetMapping("/retention/accounts")
    public String accounts() {
        // 데이터 바인딩 없이 스켈레톤만 출력
        return "retention/accounts";
    }

    /** 포트폴리오 (retention-02) */
    @GetMapping("/retention/portfolio")
    public String portfolio(@RequestParam(required = false) Long accountId, Model model) {
        // 아직 데이터 없음. 나중에 accountId 넘겨서 바인딩할 때 사용
        model.addAttribute("accountId", accountId);
        return "retention/portfolio";
    }

    /** 거래내역 (retention-04/05) */
    @GetMapping("/retention/tx")
    public String tx(@RequestParam(required = false) Long accountId, Model model) {
        model.addAttribute("accountId", accountId);
        return "retention/tx";
    }

    /** 예외/빈 상태 (retention-06) */
    @GetMapping("/retention/empty")
    public String empty() {
        return "retention/empty";
    }

    /** 보유 현황 분석 (retention-analysis-01/02/03) */
    @GetMapping("/retention/analysis")
    public String analysis() {
        return "retention/analysis";
    }
}