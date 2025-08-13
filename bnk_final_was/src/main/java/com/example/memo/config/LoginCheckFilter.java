package com.example.memo.config;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class LoginCheckFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        System.out.println("🛡️ 필터 실행: " + httpRequest.getRequestURI());

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();

        // 로그인 페이지는 필터 제외
        if (requestURI.startsWith("/company/login") ||
        	    requestURI.startsWith("/company/auth/login") || // ← 이거 꼭 추가해야 함
        	    requestURI.startsWith("/css") || requestURI.startsWith("/js")) {
        	    chain.doFilter(request, response);
        	    return;
        	}


        HttpSession session = httpRequest.getSession(false);

        if (session == null || session.getAttribute("loginManager") == null) {
            // 로그인 안 되어 있으면 로그인 페이지로 이동
            httpResponse.sendRedirect("/company/login");
            return;
        }

        // 로그인 되어 있으면 계속 진행
        chain.doFilter(request, response);
    }
}