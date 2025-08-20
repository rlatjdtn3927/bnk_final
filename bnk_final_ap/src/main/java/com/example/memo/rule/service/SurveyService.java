package com.example.memo.rule.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.memo.rule.dto.AnswerDTO;
import com.example.memo.rule.dto.SurveySubmitReq;
import com.example.memo.rule.dto.SurveySubmitRes;
import com.example.memo.rule.entity.ProfileHistory;
import com.example.memo.rule.entity.ProfileResult;
import com.example.memo.rule.entity.ProfileType;
import com.example.memo.rule.repository.ProfileHistoryRepository;
import com.example.memo.rule.repository.ProfileResultRepository;
import com.example.memo.rule.repository.ProfileTypeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final ProfileTypeRepository profileTypeRepository;
    private final ProfileResultRepository profileResultRepository;
    private final ProfileHistoryRepository profileHistoryRepository;

    // 설문 평가 및 저장
    @Transactional
    public SurveySubmitRes evaluateAndSave(SurveySubmitReq req) {
        // 0) 유저 ID 꺼내기
        Long userId = Long.valueOf(req.getMeta().get("userId").toString());

        // 1) 원점수 계산 (85점 만점)
        int rawScore = calculateScore(req.getAnswers());

        // 2) 100점 환산
        int normalizedScore = (int) Math.round((double) rawScore / 85 * 100);

        // 3) profile_type 에서 성향 찾기 (환산 점수 기준)
        ProfileType type = profileTypeRepository
                .findByMinScoreLessThanEqualAndMaxScoreGreaterThanEqual(normalizedScore, normalizedScore)
                .orElseThrow(() -> new IllegalArgumentException("점수 구간에 맞는 투자성향 없음"));

        // 4) profile_result (upsert)
        ProfileResult result = profileResultRepository.findByUserId(userId)
                .orElse(new ProfileResult());
        result.setUserId(userId);
        result.setType(type);
        result.setTotalScore(normalizedScore); // 환산 점수 저장
        result.setAnalyzedAt(LocalDateTime.now());
        profileResultRepository.save(result);

        // 5) profile_history (항상 insert)
        ProfileHistory history = new ProfileHistory();
        history.setUserId(userId);
        history.setType(type);
        history.setTotalScore(normalizedScore); // 환산 점수 저장
        history.setAnalyzedAt(LocalDateTime.now());
        profileHistoryRepository.save(history);

        // 6) 응답 DTO 반환
        return SurveySubmitRes.builder()
                .score(normalizedScore)
                .type(type.getTypeName())
                .build();
    }

    // 설문 응답 점수 합산 (Q12는 어차피 0점 → 영향 없음)
    private int calculateScore(List<AnswerDTO> answers) {
        int score = 0;
        for (AnswerDTO ans : answers) {
            if (ans.getValues() != null && !ans.getValues().isEmpty()) {
                score += ans.getValues().stream()
                        .mapToInt(Integer::parseInt)
                        .sum();
            } else if (ans.getValue() != null) {
                try {
                    score += Integer.parseInt(ans.getValue());
                } catch (NumberFormatException ignored) {
                    // yes/no 문항 등 숫자 변환 불가 항목은 무시
                }
            }
        }
        return score;
    }
}
