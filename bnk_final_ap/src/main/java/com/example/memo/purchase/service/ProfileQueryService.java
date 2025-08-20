package com.example.memo.purchase.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 🔸 추가
import com.example.memo.rule.repository.ProfileHistoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileQueryService {

    private final ProfileHistoryRepository historyRepo;

    @Transactional(readOnly = true) // 🔒 읽기 전용 트랜잭션 권장
    public Result getLatestTypeByUserId(Long userId) {
        var rows = historyRepo.findLatestTypeNameAndIdByUserId(userId, PageRequest.of(0, 1));
        if (rows.isEmpty()) return null;

        Object[] r = rows.get(0);
        String typeName = (String) r[0];
        Integer riskGradeNum = (r[1] == null) ? null : ((Number) r[1]).intValue(); // 현재 typeId를 등급번호 대용

        return new Result(typeName, riskGradeNum);
    }

    public record Result(String typeName, Integer typeNo) {}
}
