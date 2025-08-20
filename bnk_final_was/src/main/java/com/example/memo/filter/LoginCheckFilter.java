package com.example.memo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class LoginCheckFilter implements Filter {

    private static final List<String> excludePrefixes = List.of(
        "/login-api/login",
        "/login-view",
        "/login-view/logout-view",
        "/user-api/register",
        "/register",
        "/favicon.ico",
        
        // 기업 로그인
        "/company/login",
        "/company/auth/login",
        
        // 관리자 로그인
        "/admin/login",
        
        // 정적 리소스
        "/css", "/js", "/images", "/webjars", "/static",

        "/purchase",
        "/purchase/trade",
        "/purchase/api/trade",
        "/purchase/api/portfolio",
        "/purchase/api/holdings",
        "/branches",
        "/chatbot",
        "/api/embeddings",
        "/api/chat"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req  = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String uri = req.getRequestURI();

        // 1) 제외 경로 통과
        if (isExcluded(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 2) 세션/권한 체크 (영역별로 다르게)
        HttpSession session = req.getSession(false);
        
        boolean isAdmin   = uri.startsWith("/admin");
        boolean isCompany = uri.startsWith("/company");

        if (session == null) {
            redirectToLogin(res, isAdmin, isCompany);
            return;
        }

        if (isAdmin) {
            if (session.getAttribute("BankEmployee") == null) {
                res.sendRedirect("/admin/login");
                return;
            }
        } else if (isCompany) {
            if (session.getAttribute("loginManager") == null) {
                res.sendRedirect("/company/login");
                return;
            }
        } else {
            if (session.getAttribute("user") == null) {
                res.sendRedirect("/login-view");
                return;
            }
        }

        chain.doFilter(request, response);
    }
    private boolean isExcluded(String uri) {
        return excludePrefixes.stream().anyMatch(uri::startsWith);
    }

    private void redirectToLogin(HttpServletResponse res, boolean admin, boolean company) throws IOException {
        if (admin)   { res.sendRedirect("/admin/login");   return; }
        if (company) { res.sendRedirect("/company/login"); return; }
        res.sendRedirect("/login-view"); // 기본(고객)
    }
}