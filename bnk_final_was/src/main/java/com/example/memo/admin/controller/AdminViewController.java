package com.example.memo.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.memo.admin.service.MenuService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final MenuService menuService;

    // /admin
    @GetMapping({"", "/"})
    public String adminRoot(Model model) {
        String category = "dashboard";              // 기본 카테고리
        model.addAttribute("activeCategory", category);
        model.addAttribute("menuItems", menuService.items(category));
        model.addAttribute("activePage", null);     // 아직 선택 없음
        model.addAttribute("content", "admin/blank"); // 경로만 전달 (레이아웃이 :: content 붙임)
        return "admin/layout";
    }

    // /admin/{category}
    @GetMapping("/{category}")
    public String category(@PathVariable String category, Model model) {
        model.addAttribute("activeCategory", category);
        model.addAttribute("menuItems", menuService.items(category));
        model.addAttribute("activePage", null);
        model.addAttribute("content", "admin/blank");
        return "admin/layout";
    }

    // /admin/{category}/{page}
    @GetMapping("/{category}/{page}")
    public String page(@PathVariable String category,
                       @PathVariable String page,
                       Model model) {
        model.addAttribute("activeCategory", category);
        model.addAttribute("menuItems", menuService.items(category));
        model.addAttribute("activePage", page);
        model.addAttribute("content", String.format("admin/%s/%s", category, page));
        return "admin/layout";
    }
}
