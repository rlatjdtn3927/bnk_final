//package com.example.memo.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//public class SecurityConfig {
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//            .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
//            .authorizeHttpRequests(auth -> auth
//                .anyRequest().permitAll() // 모든 요청 허용
//            )
//            .formLogin(form -> form.disable()); // 기본 로그인 폼 제거
//
//        return http.build();
//    }
//}
//
