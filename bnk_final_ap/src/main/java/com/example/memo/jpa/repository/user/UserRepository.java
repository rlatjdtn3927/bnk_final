package com.example.memo.jpa.repository.user;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.user.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByUserId(Long userId);
    Optional<UserEntity> findByEmail(String email);
    
    Optional<UserEntity> findByRrn(String rrnEnc);
    
    Optional<UserEntity> findByNameAndBirthDate(String name, LocalDate birthDate);
}
