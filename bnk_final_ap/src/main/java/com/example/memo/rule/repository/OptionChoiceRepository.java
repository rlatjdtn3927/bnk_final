package com.example.memo.rule.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.rule.entity.OptionChoice;

public interface OptionChoiceRepository extends JpaRepository<OptionChoice, Long> {
}
