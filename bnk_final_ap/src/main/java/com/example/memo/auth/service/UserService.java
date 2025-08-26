package com.example.memo.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.auth.dto.UserRegisterDTO;
import com.example.memo.company.service.CryptoService;
import com.example.memo.jpa.entity.user.UserEntity;
import com.example.memo.jpa.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;   // BCrypt
    private final CryptoService cryptoService;       // AES

    @Transactional
    public Long register(UserRegisterDTO dto) {
        if (isBlank(dto.getUsername()) || isBlank(dto.getPassword())
                || isBlank(dto.getEmail()) || isBlank(dto.getGender())
                || dto.getBirthDate() == null || isBlank(dto.getRrn())) {
            throw new IllegalArgumentException("BAD_REQUEST");
        }

        userRepository.findByUsername(dto.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("DUPLICATE_USERNAME");
        });
        userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("DUPLICATE_EMAIL");
        });

        String rrnRaw = dto.getRrn().replaceAll("[^0-9]", "");
        if (rrnRaw.length() != 13) throw new IllegalArgumentException("BAD_REQUEST");
        System.out.println("test user rrn: " + rrnRaw);
        
        
        String rrnEnc = cryptoService.encrypt(rrnRaw);                 // ✅ 암호화
        String pwHash = passwordEncoder.encode(dto.getPassword());     // ✅ bcrypt

        UserEntity u = new UserEntity();
        u.setUsername(dto.getUsername());
        u.setPasswordHash(pwHash);
        u.setName(dto.getNickname());
        u.setGender(dto.getGender());
        u.setEmail(dto.getEmail());
        u.setJob(dto.getJob());
        u.setBirthDate(dto.getBirthDate());
        u.setRrn(rrnEnc); // ✅ 암호문 저장
        u.setCreated_at(LocalDateTime.now());
        u.setUpdated_at(LocalDateTime.now());

        userRepository.save(u);
        return u.getUserId();
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    
    public String getUserName(Long userId) {
        return userRepository.findById(userId)
                .map(UserEntity::getName)  
                .orElse("알 수 없음");
    }
}
