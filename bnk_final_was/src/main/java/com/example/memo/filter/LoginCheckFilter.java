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
        "/company/login",
        "/company/auth/login",
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
        boolean isCompany = uri.startsWith("/company");

        if (session == null) {
            redirectToLogin(res, isCompany);
            return;
        }

        if (isCompany) {
            // 기업뱅킹 영역: loginManager만 확인
            if (session.getAttribute("loginManager") == null) {
                redirectToLogin(res, true);
                return;
            }
        } else {
            // 고객 영역: user만 확인
            if (session.getAttribute("user") == null) {
                redirectToLogin(res, false);
                return;
            }
        }

        // 3) 통과
        chain.doFilter(request, response);
    }

    private boolean isExcluded(String uri) {
        return excludePrefixes.stream().anyMatch(uri::startsWith);
    }

    private void redirectToLogin(HttpServletResponse res, boolean company) throws IOException {
        res.sendRedirect(company ? "/company/login" : "/login-view");
    }
}