package com.example.memo.jpa.repository.chatbot;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.chatbot.ChatLog;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
}
