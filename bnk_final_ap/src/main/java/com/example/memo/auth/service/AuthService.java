package com.example.memo.auth.service;

import com.example.memo.auth.dto.UserLoginResultDTO;

public interface AuthService {
    UserLoginResultDTO loginUserByUsername(String username, String rawPassword);
    UserLoginResultDTO loginUserByUserId(Long userId, String rawPassword);
}
