package com.example.memo.auth.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserRegisterDTO {
    private String username;
    private String password;
    private String nickname;
    private String gender;          // "M"/"F"
    private String email;
    private String job;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;    // WAS에서 yyyy-MM-dd 문자열 → LocalDate

    private String rrn;             // 숫자 13자리
}
