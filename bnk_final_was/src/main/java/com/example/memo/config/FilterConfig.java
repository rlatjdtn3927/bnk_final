package com.example.memo.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.memo.filter.LoginCheckFilter;

@Configuration
public class FilterConfig {

//    @Bean
    public FilterRegistrationBean<LoginCheckFilter> loginCheckFilter() {
        FilterRegistrationBean<LoginCheckFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new LoginCheckFilter());
        bean.addUrlPatterns("/*"); // 전역
        bean.setOrder(1);          // 우선순위(낮을수록 먼저)
        return bean;
    }
}
