package com.example.memo.admin.controller;

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

    /** 첫 진입: 풀페이지 (레이아웃 + 초기 콘텐츠) */
    @GetMapping
    public String home(Model model) {
        String cat = "dashboard";
        String code = Optional.ofNullable(sidebar.defaultOf(cat))
                .orElseGet(() -> sidebar.list(cat).stream()
                        .findFirst().map(Subcat::getCode).orElse(null));
        Subcat s = sidebar.find(code).orElseThrow();

        model.addAttribute("activeCategory", cat);
        model.addAttribute("subcats", sidebar.list(cat));
        model.addAttribute("activeCode", code);
        model.addAttribute("initialView", s.getView()); // ← 초기 콘텐츠 바로 보이게
        model.addAttribute("pageTitle", "관리자 - " + sidebar.nameOf(cat) + " - " + s.getLabel());
        return "admin/layout";
    }

    /** 헤더 클릭: 카테고리 전환 (사이드바 OOB + 초기 콘텐츠) */
    @GetMapping("/ui/category/{category}")
    public String switchCategory(@PathVariable("category") String category,
                                 HttpServletRequest req,
                                 Model model) {
        boolean hx = "true".equalsIgnoreCase(req.getHeader("HX-Request"));

        String code = Optional.ofNullable(sidebar.defaultOf(category))
                .orElseGet(() -> sidebar.list(category).stream()
                        .findFirst().map(Subcat::getCode).orElse(null));
        if (code == null) {
            // 해당 카테고리에 서브카테고리가 하나도 없으면 안전 폴백
            return hx ? "admin/category_switch" : "redirect:/admin";
        }

        Subcat s = sidebar.find(code).orElseThrow();

        model.addAttribute("activeCategory", category);
        model.addAttribute("subcats", sidebar.list(category));
        model.addAttribute("activeCode", code);
        model.addAttribute("initialView", s.getView());
        model.addAttribute("pageTitle", "관리자 - " + sidebar.nameOf(category) + " - " + s.getLabel());

        if (!hx) return "redirect:/admin/page/" + code; // 새로고침/직접 접근은 풀페이지
        return "admin/category_switch";                  // HTMX: 사이드바 OOB + 콘텐츠
    }

    /** 사이드바 클릭: 페이지 전환 (같은 템플릿으로 사이드바 OOB + 콘텐츠) */
    @GetMapping("/ui/page/{code}")
    public String switchPage(@PathVariable("code") String code,
                             HttpServletRequest req,
                             Model model) {
        boolean hx = "true".equalsIgnoreCase(req.getHeader("HX-Request"));
        Subcat s = sidebar.find(code).orElseThrow();
        String cat = s.getCategory();

        model.addAttribute("activeCategory", cat);
        model.addAttribute("subcats", sidebar.list(cat));
        model.addAttribute("activeCode", code);
        model.addAttribute("initialView", s.getView());
        model.addAttribute("pageTitle", "관리자 - " + sidebar.nameOf(cat) + " - " + s.getLabel());

        if (!hx) return "redirect:/admin/page/" + code; // 비 HTMX → 풀페이지
        return "admin/category_switch";                  // HTMX → 공용 템플릿
    }

    /** URL 직접 접근/새로고침: 풀페이지 (레이아웃 + 초기 콘텐츠) */
    @GetMapping("/page/{code}")
    public String deepLink(@PathVariable("code") String code, Model model) {
        Subcat s = sidebar.find(code).orElseThrow();
        String cat = s.getCategory();

        model.addAttribute("activeCategory", cat);
        model.addAttribute("subcats", sidebar.list(cat));
        model.addAttribute("activeCode", code);
        model.addAttribute("initialView", s.getView()); // ← 풀페이지에서도 바로 해당 콘텐츠
        model.addAttribute("pageTitle", "관리자 - " + sidebar.nameOf(cat) + " - " + s.getLabel());
        return "admin/layout";
    }
}
