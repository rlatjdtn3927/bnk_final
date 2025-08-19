// WAS - src/main/java/com/example/memo/purchase/dto/Step1AccountsRes.java
package com.example.memo.purchase.dto;

import java.util.List;
import lombok.*;
//was Step1 전용 API (계좌목록 조회) 응답 DTO
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Step1AccountsRes {
    private boolean hasIrp;
    private AccountItem irp;           // IRP 1건(필수)
    private boolean hasDc;
    private List<AccountItem> dcList;  // DC계좌는 여러 개 있을 수 있다(A회사에서 일하다가 B회사로 이동)
    private String message;            // 안내문구(예: DC 없음)

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AccountItem {
        private String accountId;      // IRP: irp_acct_no, DC: account_no
        private String accountType;    // "IRP" | "DC"
        private String displayName;    // 마스킹 계좌번호 등
        private String status;         // NORMAL/CLOSED 등(선택)
    }
}
