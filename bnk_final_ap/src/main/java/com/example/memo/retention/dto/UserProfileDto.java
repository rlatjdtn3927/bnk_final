package com.example.memo.retention.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class UserProfileDto {
    private boolean ok;
    private Long userId;
    private String name;     // 회원 이름
    private String username; // 로그인 아이디
    private String email;
}
