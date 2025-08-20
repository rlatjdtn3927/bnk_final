package com.example.memo.rule.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.rule.entity.ProfileResult;

public interface ProfileResultRepository extends JpaRepository<ProfileResult, Long> {
    Optional<ProfileResult> findByUserId(Long userId);
}
