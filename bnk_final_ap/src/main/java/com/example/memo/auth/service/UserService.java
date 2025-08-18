// com.example.memo.auth.service.UserService
package com.example.memo.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.auth.dto.UserRegisterDTO;
import com.example.memo.auth.entity.UserEntity;
import com.example.memo.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long register(UserRegisterDTO dto) {
        // 중복 체크
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("DUPLICATE_USERNAME");
        }
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("DUPLICATE_EMAIL");
        }

        UserEntity u = new UserEntity();
        u.setUsername(dto.getUsername());
        u.setPasswordHash(passwordEncoder.encode(dto.getPassword())); // ✅ bcrypt 저장
        u.setName(dto.getNickname());
        u.setGender(dto.getGender());
        u.setEmail(dto.getEmail());
        u.setJob(dto.getJob());
        u.setBirthDate(dto.getBirthDate());
        u.setRrn("0000000000000"); // 필요시 실제 로직으로 교체
        u.setCreated_at(LocalDateTime.now());
        u.setUpdated_at(LocalDateTime.now());

        userRepository.save(u);
        return u.getUserId();
    }
}
