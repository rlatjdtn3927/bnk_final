package com.example.memo.auth.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserLoginResultDTO {
    private Long userId;
    private String username;
    private String displayName;
    private List<String> roles;
}
