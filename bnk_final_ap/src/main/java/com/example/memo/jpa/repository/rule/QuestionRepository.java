package com.example.memo.jpa.repository.rule;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.rule.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
