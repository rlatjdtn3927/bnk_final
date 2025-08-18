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
		    "/register-form",
		    "/favicon.ico"
		);

		private static final String[] staticPrefixes = { "/css", "/js", "/images", "/webjars" };

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		        throws IOException, ServletException {

		    HttpServletRequest req = (HttpServletRequest) request;
		    HttpServletResponse res = (HttpServletResponse) response;
		    String uri = req.getRequestURI();

		    // 정적 리소스는 통과
		    for (String p : staticPrefixes) {
		        if (uri.startsWith(p)) { chain.doFilter(request, response); return; }
		    }
		    // 화이트리스트 통과
		    if (excludeUrls.contains(uri)) { chain.doFilter(request, response); return; }

		    // 보호 대상: 메인/설문 화면/설문 API
		    boolean needsLogin =
		            uri.startsWith("/main") ||
		            uri.startsWith("/survey-view") ||
		            uri.startsWith("/survey-api");

		    if (needsLogin) {
		        HttpSession session = req.getSession(false);
		        if (session == null || session.getAttribute("user") == null) {
		            res.sendRedirect("/login-view");
		            return;
		        }
		    }
		    chain.doFilter(request, response);
		}

}
