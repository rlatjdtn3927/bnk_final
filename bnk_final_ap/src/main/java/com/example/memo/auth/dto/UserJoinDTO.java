package com.example.memo.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserJoinDTO {
	private String username;
    private String nickname;
    private String gender;
    private String birthDate;
    private String email;
    private String job;
    private String password;
    private String confirmPassword;
    private Boolean agreeTerms;
}
