package com.example.memo.auth.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LoginDTO {
    private String username; // 기본 경로
    private String password;
}
