package com.example.memo.auth.dto;

import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class ApiResult<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, "OK", "성공", data);
    }
    public static <T> ApiResult<T> fail(String code, String message) {
        return new ApiResult<>(false, code, message, null);
    }
}
