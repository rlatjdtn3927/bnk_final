package com.example.memo.jpa.repository.rule;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.rule.ProfileType;

public interface ProfileTypeRepository extends JpaRepository<ProfileType, Long> {
    Optional<ProfileType> findByMinScoreLessThanEqualAndMaxScoreGreaterThanEqual(Integer minScore, Integer maxScore);
}
