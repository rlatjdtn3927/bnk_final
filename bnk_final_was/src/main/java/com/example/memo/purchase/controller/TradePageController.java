// src/main/java/com/example/memo/purchase/controller/TradePageController.java
package com.example.memo.purchase.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/purchase/trade")
@RequiredArgsConstructor
public class TradePageController {

    // ----- STEP 1: 계좌/잔액/사전체크 -----
    @GetMapping("/{flow}/step1")
    public String step1(@PathVariable("flow") String flow,
                        @RequestParam(name="userId", required = false) String userId,
                        @RequestParam(name="accountType", required = false) String accountType,
                        @RequestParam(name="acountId", required = false) String acountId,
                        Model model) {
        model.addAttribute("flow", flow.toUpperCase());
        model.addAttribute("userId", userId == null ? "" : userId);
        model.addAttribute("accountType", accountType == null ? "IRP" : accountType);
        model.addAttribute("acountId", acountId == null ? "" : acountId);
        return "purchase/trade/step1_check";
    }

    @PostMapping("/{flow}/step1/next")
    public String step1Next(@PathVariable("flow") String flow,
                            @RequestParam("userId") String userId,
                            @RequestParam("accountType") String accountType,
                            @RequestParam("acountId") String acountId) {
        String qs = String.format("?userId=%s&accountType=%s&acountId=%s",
                UriUtils.encode(userId, StandardCharsets.UTF_8),
                UriUtils.encode(accountType, StandardCharsets.UTF_8),
                UriUtils.encode(acountId, StandardCharsets.UTF_8));
        return "redirect:/purchase/trade/" + flow + "/step2" + qs;
    }

    // ----- STEP 2: 상품 선택 & (MATURITY 전용: 원본 예금/전환일 선택) -----
    @GetMapping("/{flow}/step2")
    public String step2(@PathVariable("flow") String flow,
                        @RequestParam("userId") String userId,
                        @RequestParam("accountType") String accountType,
                        @RequestParam("acountId") String acountId,
                        @RequestParam(name="items", required = false) String items,
                        // MATURITY 옵션
                        @RequestParam(name="maturityProductId", required = false) String maturityProductId,
                        @RequestParam(name="changeDate", required = false) String changeDate,
                        Model model) {
        model.addAttribute("flow", flow.toUpperCase());
        model.addAttribute("userId", userId);
        model.addAttribute("accountType", accountType);
        model.addAttribute("acountId", acountId);
        model.addAttribute("items", items == null ? "" : items);
        model.addAttribute("maturityProductId", maturityProductId == null ? "" : maturityProductId);
        model.addAttribute("changeDate", changeDate == null ? "" : changeDate);
        return "purchase/trade/step2_select";
    }

    @PostMapping("/{flow}/step2/next")
    public String step2Next(@PathVariable("flow") String flow,
                            @RequestParam("userId") String userId,
                            @RequestParam("accountType") String accountType,
                            @RequestParam("acountId") String acountId,
                            @RequestParam("itemsJson") String itemsJson,
                            // MATURITY 옵션
                            @RequestParam(name="maturityProductId", required = false) String maturityProductId,
                            @RequestParam(name="changeDate", required = false) String changeDate) {
        String qs = String.format("?userId=%s&accountType=%s&acountId=%s&items=%s&maturityProductId=%s&changeDate=%s",
                UriUtils.encode(userId, StandardCharsets.UTF_8),
                UriUtils.encode(accountType, StandardCharsets.UTF_8),
                UriUtils.encode(acountId, StandardCharsets.UTF_8),
                UriUtils.encode(itemsJson, StandardCharsets.UTF_8),
                UriUtils.encode(maturityProductId == null ? "" : maturityProductId, StandardCharsets.UTF_8),
                UriUtils.encode(changeDate == null ? "" : changeDate, StandardCharsets.UTF_8));
        return "redirect:/purchase/trade/" + flow + "/step3" + qs;
    }

    // ----- STEP 3: 서류 동의 -----
    @GetMapping("/{flow}/step3")
    public String step3(@PathVariable("flow") String flow,
                        @RequestParam("userId") String userId,
                        @RequestParam("accountType") String accountType,
                        @RequestParam("acountId") String acountId,
                        @RequestParam("items") String items,
                        @RequestParam(name="docs", required = false) String docs,
                        // MATURITY 옵션
                        @RequestParam(name="maturityProductId", required = false) String maturityProductId,
                        @RequestParam(name="changeDate", required = false) String changeDate,
                        Model model) {
        model.addAttribute("flow", flow.toUpperCase());
        model.addAttribute("userId", userId);
        model.addAttribute("accountType", accountType);
        model.addAttribute("acountId", acountId);
        model.addAttribute("items", items);
        model.addAttribute("docs", docs == null ? "" : docs);
        model.addAttribute("maturityProductId", maturityProductId == null ? "" : maturityProductId);
        model.addAttribute("changeDate", changeDate == null ? "" : changeDate);
        return "purchase/trade/step3_documents";
    }

    @PostMapping("/{flow}/step3/next")
    public String step3Next(@PathVariable("flow") String flow,
                            @RequestParam("userId") String userId,
                            @RequestParam("accountType") String accountType,
                            @RequestParam("acountId") String acountId,
                            @RequestParam("items") String items,
                            @RequestParam(name="docs", required = false) String docs,
                            // MATURITY 옵션
                            @RequestParam(name="maturityProductId", required = false) String maturityProductId,
                            @RequestParam(name="changeDate", required = false) String changeDate) {
        String qs = String.format("?userId=%s&accountType=%s&acountId=%s&items=%s&docs=%s&maturityProductId=%s&changeDate=%s",
                UriUtils.encode(userId, StandardCharsets.UTF_8),
                UriUtils.encode(accountType, StandardCharsets.UTF_8),
                UriUtils.encode(acountId, StandardCharsets.UTF_8),
                UriUtils.encode(items, StandardCharsets.UTF_8),
                UriUtils.encode(docs == null ? "" : docs, StandardCharsets.UTF_8),
                UriUtils.encode(maturityProductId == null ? "" : maturityProductId, StandardCharsets.UTF_8),
                UriUtils.encode(changeDate == null ? "" : changeDate, StandardCharsets.UTF_8));
        return "redirect:/purchase/trade/" + flow + "/step4" + qs;
    }

    // ----- STEP 4: 미리보기 -----
    @GetMapping("/{flow}/step4")
    public String step4(@PathVariable("flow") String flow,
                        @RequestParam("userId") String userId,
                        @RequestParam("accountType") String accountType,
                        @RequestParam("acountId") String acountId,
                        @RequestParam("items") String items,
                        @RequestParam(name="docs", required = false) String docs,
                        // MATURITY 옵션
                        @RequestParam(name="maturityProductId", required = false) String maturityProductId,
                        @RequestParam(name="changeDate", required = false) String changeDate,
                        Model model) {
        model.addAttribute("flow", flow.toUpperCase());
        model.addAttribute("userId", userId);
        model.addAttribute("accountType", accountType);
        model.addAttribute("acountId", acountId);
        model.addAttribute("items", items);
        model.addAttribute("docs", docs == null ? "" : docs);
        model.addAttribute("maturityProductId", maturityProductId == null ? "" : maturityProductId);
        model.addAttribute("changeDate", changeDate == null ? "" : changeDate);
        return "purchase/trade/step4_review";
    }

    @PostMapping("/{flow}/step4/next")
    public String step4Next(@PathVariable("flow") String flow,
                            @RequestParam("userId") String userId,
                            @RequestParam("accountType") String accountType,
                            @RequestParam("acountId") String acountId,
                            @RequestParam("items") String items,
                            @RequestParam(name="docs", required = false) String docs,
                            // MATURITY 옵션
                            @RequestParam(name="maturityProductId", required = false) String maturityProductId,
                            @RequestParam(name="changeDate", required = false) String changeDate) {
        String qs = String.format("?userId=%s&accountType=%s&acountId=%s&items=%s&docs=%s&maturityProductId=%s&changeDate=%s",
                UriUtils.encode(userId, StandardCharsets.UTF_8),
                UriUtils.encode(accountType, StandardCharsets.UTF_8),
                UriUtils.encode(acountId, StandardCharsets.UTF_8),
                UriUtils.encode(items, StandardCharsets.UTF_8),
                UriUtils.encode(docs == null ? "" : docs, StandardCharsets.UTF_8),
                UriUtils.encode(maturityProductId == null ? "" : maturityProductId, StandardCharsets.UTF_8),
                UriUtils.encode(changeDate == null ? "" : changeDate, StandardCharsets.UTF_8));
        return "redirect:/purchase/trade/" + flow + "/step5" + qs;
    }

    // ----- STEP 5: 비밀번호 입력 & 적용 -----
    @GetMapping("/{flow}/step5")
    public String step5(@PathVariable("flow") String flow,
                        @RequestParam("userId") String userId,
                        @RequestParam("accountType") String accountType,
                        @RequestParam("acountId") String acountId,
                        @RequestParam("items") String items,
                        @RequestParam(name="docs", required = false) String docs,
                        // MATURITY 옵션
                        @RequestParam(name="maturityProductId", required = false) String maturityProductId,
                        @RequestParam(name="changeDate", required = false) String changeDate,
                        Model model) {
        model.addAttribute("flow", flow.toUpperCase());
        model.addAttribute("userId", userId);
        model.addAttribute("accountType", accountType);
        model.addAttribute("acountId", acountId);
        model.addAttribute("items", items);
        model.addAttribute("docs", docs == null ? "" : docs);
        model.addAttribute("maturityProductId", maturityProductId == null ? "" : maturityProductId);
        model.addAttribute("changeDate", changeDate == null ? "" : changeDate);
        return "purchase/trade/step5_apply";
    }

    @PostMapping("/{flow}/step5/done")
    public String step5Done(@PathVariable("flow") String flow) {
        return "redirect:/purchase/trade/" + flow + "/done";
    }

    // ----- 완료 -----
    @GetMapping("/{flow}/done")
    public String done(@PathVariable("flow") String flow, Model model) {
        model.addAttribute("flow", flow.toUpperCase());
        return "purchase/trade/done";
    }
}
