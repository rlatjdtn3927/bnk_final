package com.example.memo.company.dto;

import java.time.LocalDateTime;

import com.example.memo.jpa.entity.company.DcAccountRequest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountRequestListDto {
    private Long requestId;
    private String requestStatus;
    private LocalDateTime requestedAt;

    // 가입자 정보
    private Long memberId;
    private String memberName;

    // 요청한 기업 담당자 정보
    private Long companyManagerId;
    private String companyManagerName;
    private String companyName;

    public static AccountRequestListDto fromEntity(DcAccountRequest entity) {
        return AccountRequestListDto.builder()
                .requestId(entity.getId())
                .requestStatus(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .memberId(entity.getDcMember().getId())
                .memberName(entity.getDcMember().getName())
                .companyManagerId(entity.getRequestedBy().getId())
                .companyManagerName(entity.getRequestedBy().getName())
                .companyName(entity.getRequestedBy().getCompany().getName()) // 회사 이름 추가
                .build();
    }
}