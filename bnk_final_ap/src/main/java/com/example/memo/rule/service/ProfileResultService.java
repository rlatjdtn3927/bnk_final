package com.example.memo.rule.service;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ProfileResultService {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getProfileTypeByUserId(Long userId) {
        String sql = """
            SELECT t.type_id, t.type_name
              FROM profile_result r
              JOIN profile_type t ON r.type_id = t.type_id
             WHERE r.user_id = ?
            """;

        try {
            return jdbcTemplate.queryForMap(sql, userId);
        } catch (Exception e) {
            return null; // 조회 실패 시 null 반환
        }
    }
}
