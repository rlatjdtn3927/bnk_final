package com.example.memo.admin.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.example.memo.admin.service.AdminPageDataService;
import com.example.memo.admin.service.SidebarService;
import com.example.memo.admin.service.Subcat;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final SidebarService sidebar;
    private final AdminPageDataService pageData; // ★ 주입

    /**
     * 공통 모델 속성 준비 메소드
     * 페이지 코드(code)를 기반으로 View 렌더링에 필요한 모든 모델 속성을 준비
     *
     * @param code  페이지 고유 코드
     * 
     */
    private void prepareModelForView(String code, Model model) {
        Subcat s = sidebar.find(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));
        String cat = s.getCategory();

        model.addAttribute("activeCategory", cat); // 현재 활성화된 카테고리
        model.addAttribute("subcats", sidebar.list(cat)); // 현재 카테고리의 서브메뉴 목록
        model.addAttribute("activeCode", code); // 현재 활성화된 페이지 코드
        model.addAttribute("pageView", s.getView()); // 현재 페이지의 템플릿 경로
        model.addAttribute("pageTitle", "관리자 - " + sidebar.nameOf(cat) + " - " + s.getLabel());

        // 추가: 헤더에 쓸 카테고리들
        model.addAttribute("headerCategories", sidebar.getHeaderCategories());
        
        pageData.populateFor(code, model); // ★ 여기서 total-assets면 totals 주입

    }
    /**
     * 관리자 홈 (첫 진입)
     * 기본 페이지로 리다이렉트하여 URL을 표준화
     */
    @GetMapping
    public String home() {
        String defaultCode = sidebar.defaultOf("dashboard"); // 기본 대시보드 코드 찾기
        return "redirect:/admin/page/" + defaultCode;
    }

    /**
     * URL 직접 접근 / 새로고침을 위한 풀 페이지 렌더링
     * 이 엔드포인트가 모든 풀 페이지 요청의 기준점
     */
    @GetMapping("/page/{code}")
    public String getFullPage(@PathVariable("code") String code, Model model) {
        prepareModelForView(code, model);
        return "admin/layout"; // 전체 레이아웃 렌더링
    }

    /**
     * 헤더의 카테고리 클릭 시
     * 해당 카테고리의 기본 페이지로 HTMX 조각들을 반환
     */
    @GetMapping("/ui/category/{category}")
    public String switchCategory(
            @PathVariable("category") String category,
            @RequestHeader(name = "HX-Request", required = false) String hx,  // ← 이름 명시!
            Model model) {

        String code = Optional.ofNullable(sidebar.defaultOf(category))
                .orElseGet(() -> sidebar.list(category).stream()
                        .findFirst().map(Subcat::getCode).orElse(null));
        if (code == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        if (hx == null) return "redirect:/admin/page/" + code;  // HTMX 아님 → 풀페이지

        prepareModelForView(code, model);
        return "admin/category_switch";
    }

    /**
     * 사이드바 메뉴 클릭 시
     * 요청된 페이지의 HTMX 조각들을 반환
     */
    @GetMapping("/ui/page/{code}")
    public String switchPage(
            @PathVariable("code") String code,
            @RequestHeader(name = "HX-Request", required = false) String hx,  // ← 이름 명시!
            Model model) {

        if (hx == null) return "redirect:/admin/page/" + code;  // HTMX 아님 → 풀페이지

        prepareModelForView(code, model);
        return "admin/category_switch";
    }
    
    
}
