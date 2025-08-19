package com.example.memo.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserJoinDTO {
    private String username;
    private String nickname;
    private String gender;      // "M" / "F"
    private String email;
    private String job;
    private String birthDate;   // yyyy-MM-dd (WAS→AP 직렬화용 문자열)
    private String password;
    private String rrn;         // 프론트에서 합쳐 보냄
}
