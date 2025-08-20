package com.example.memo.auth.service;

import java.util.List;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.memo.auth.dto.UserLoginResultDTO;
import com.example.memo.jpa.entity.user.UserEntity;
import com.example.memo.jpa.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // BCryptPasswordEncoder 주입

    @Override
    public UserLoginResultDTO loginUserByUsername(String username, String rawPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("INVALID_CREDENTIALS");
        }
        return toResult(user);
    }

    @Override
    public UserLoginResultDTO loginUserByUserId(Long userId, String rawPassword) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("INVALID_CREDENTIALS");
        }
        return toResult(user);
    }

    private UserLoginResultDTO toResult(UserEntity user) {
        UserLoginResultDTO dto = new UserLoginResultDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getName());
        dto.setRoles(List.of("USER"));
        return dto;
    }
}
