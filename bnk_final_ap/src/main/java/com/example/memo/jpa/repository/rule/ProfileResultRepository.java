package com.example.memo.jpa.repository.rule;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.rule.ProfileResult;

public interface ProfileResultRepository extends JpaRepository<ProfileResult, Long> {
    Optional<ProfileResult> findByUserId(Long userId);
}
