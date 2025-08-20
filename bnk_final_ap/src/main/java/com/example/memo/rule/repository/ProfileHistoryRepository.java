package com.example.memo.rule.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.rule.entity.ProfileHistory;

public interface ProfileHistoryRepository extends JpaRepository<ProfileHistory, Long> {
}
