package com.example.memo.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.ChatLog;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
}
