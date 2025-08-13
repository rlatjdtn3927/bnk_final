package com.example.memo.admin.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.memo.admin.service.SidebarService;
import com.example.memo.admin.service.Subcat;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final SidebarService sidebar;

    /** 첫 진입: 풀페이지 */
    @GetMapping
    public String home(Model model) {
        String cat = "dashboard";
        String code = sidebar.defaultOf(cat);
        List<Subcat> subs = sidebar.list(cat);

        model.addAttribute("activeCategory", cat);
        model.addAttribute("subcats", subs);
        model.addAttribute("activeCode", code);
        model.addAttribute("pageTitle", "관리자 - " + sidebar.nameOf(cat));
        return "admin/layout";
    }

    /** 헤더 클릭: 사이드바 OOB + 초기 콘텐츠 */
    @GetMapping("/ui/category/{category}")
    public String switchCategory(@PathVariable("category") String category,
                                 HttpServletRequest req,
                                 Model model) {
        boolean hx = "true".equalsIgnoreCase(req.getHeader("HX-Request"));
        String code = Optional.ofNullable(sidebar.defaultOf(category))
                .orElseGet(() -> sidebar.list(category).stream()
                        .findFirst().map(Subcat::getCode)
                        .orElse(null));
        if (!hx) {
            // 비 HTMX 요청(새로고침/직접접근) → 풀페이지로
            return "redirect:/admin/page/" + code;
        }

        var s = sidebar.find(code).orElseThrow();
        model.addAttribute("activeCategory", category);
        model.addAttribute("subcats", sidebar.list(category));
        model.addAttribute("activeCode", code);
        model.addAttribute("initialView", s.getView());
        model.addAttribute("pageTitle", "관리자 - " + sidebar.nameOf(category) + " - " + s.getLabel());
        return "admin/category_switch";
    }

    /** 사이드바 클릭: 콘텐츠만 조각 반환 */
    @GetMapping("/ui/page/{code}")
    public String loadPage(@PathVariable("code") String code,
                           HttpServletRequest req,
                           Model model) {
        boolean hx = "true".equalsIgnoreCase(req.getHeader("HX-Request"));
        if (!hx) {
            return "redirect:/admin/page/" + code; // 비 HTMX 요청 폴백
        }
        var s = sidebar.find(code).orElseThrow();
        model.addAttribute("activeCode", code);
        model.addAttribute("pageTitle", "관리자 - " + s.getLabel());
        return s.getView() + " :: content";
    }

    /** URL 직접 접근/새로고침: 풀페이지 (폴백) */
    @GetMapping("/page/{code}")
    public String deepLink(@PathVariable("code") String code, Model model) {
        String cat = sidebar.categoryOf(code);
        model.addAttribute("activeCategory", cat);
        model.addAttribute("subcats", sidebar.list(cat));
        model.addAttribute("activeCode", code);
        model.addAttribute("pageTitle",
            "관리자 - " + sidebar.find(code).map(Subcat::getLabel).orElse(""));
        return "admin/layout";
    }
}