package com.example.memo.irp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
	private Long userId;   // FK로 참조
	private String username;
    private String name;
    private String email;
}
