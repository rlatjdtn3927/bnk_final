// WAS - src/main/java/com/example/memo/purchase/dto/trade/Step4DocsRes.java
package com.example.memo.purchase.dto.trade;

import java.util.List;
import java.util.Map;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Step4DocsRes {
    private String flow;                   // RESERVE | MATURITY | CHANGE
    private List<FileUrlDto> docs;         // 문서 목록(세션의 fileUrlList에서 선택 상품만 필터)
    private Map<String, Integer> ratios;   // RESERVE에서만: prodId -> 비율(%)
    private String message;                // 안내/오류 메시지
}
