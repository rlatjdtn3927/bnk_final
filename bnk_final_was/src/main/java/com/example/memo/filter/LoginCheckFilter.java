package com.example.memo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class LoginCheckFilter implements Filter {

    private static final List<String> excludeUrls = List.of(
        "/login-api/login",
        "/login-view",
        "/login-view/logout-view",
        "/user-api/register",
        "/register",
        "/favicon.ico",
        "/company/login"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        String requestURI = httpReq.getRequestURI();

        // 1) 제외 경로라면 바로 통과
        if (isExcluded(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // 2) 세션에서 로그인 체크
        HttpSession session = httpReq.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            // 회사 전용 경로 요청 시 → /company/login 으로
            if (requestURI.startsWith("/company")) {
                httpRes.sendRedirect("/company/login");
            } else {
                // 기본은 고객용 로그인 페이지
                httpRes.sendRedirect("/login-view");
            }
            return;
        }

        // 3) 로그인 되어있으면 다음 필터로
        chain.doFilter(request, response);
    }

    private boolean isExcluded(String uri) {
        return excludeUrls.stream().anyMatch(uri::startsWith);
    }
}
