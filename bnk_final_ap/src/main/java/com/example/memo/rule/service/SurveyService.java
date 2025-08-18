package com.example.memo.rule.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.memo.rule.dto.AnswerDTO;
import com.example.memo.rule.dto.SurveySubmitReq;
import com.example.memo.rule.dto.SurveySubmitRes;

@Service
public class SurveyService {

    public SurveySubmitRes evaluateAndSave(SurveySubmitReq req) {
        // 1) 점수 계산
        int totalScore = calculateScore(req.getAnswers());

        // 2) 점수 → 성향 분류
        String typeName = classifyByScore(totalScore);

        // 3) DB 저장 (profile_result, profile_history 등) ← TODO: Repository 연결 필요
        // 예: profileHistoryRepository.save(new ProfileHistory(...));

        return SurveySubmitRes.builder()
                .score(totalScore)
                .type(typeName)
                .build();
    }

    private int calculateScore(List<AnswerDTO> answers) {
        int score = 0;
        for (AnswerDTO ans : answers) {
            if (ans.getValues() != null && !ans.getValues().isEmpty()) {
                // 체크박스 문항 합산
                score += ans.getValues().stream().mapToInt(Integer::parseInt).sum();
            } else if (ans.getValue() != null) {
                try {
                    score += Integer.parseInt(ans.getValue());
                } catch (NumberFormatException ignored) {
                    // q12 같은 yes/no 문항은 점수 없음
                }
            }
        }
        return score;
    }

    private String classifyByScore(int score) {
        if (score >= 70) return "공격투자형";
        else if (score >= 55) return "적극투자형";
        else if (score >= 40) return "위험중립형";
        else if (score >= 25) return "안정추구형";
        else return "안정형";
    }
}
