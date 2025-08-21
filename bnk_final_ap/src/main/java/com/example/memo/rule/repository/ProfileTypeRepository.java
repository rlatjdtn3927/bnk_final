package com.example.memo.rule.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.rule.entity.ProfileType;

public interface ProfileTypeRepository extends JpaRepository<ProfileType, Long> {
    Optional<ProfileType> findByMinScoreLessThanEqualAndMaxScoreGreaterThanEqual(Integer minScore, Integer maxScore);
}
