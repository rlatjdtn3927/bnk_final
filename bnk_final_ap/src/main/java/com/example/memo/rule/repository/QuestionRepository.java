package com.example.memo.rule.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.rule.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
