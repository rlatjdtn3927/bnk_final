package com.example.memo.company.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class DcMemberRegisterResultDto {

    private int total;
    private int success;
    private int failed;
    private List<ErrorDetail> errors = new ArrayList<>();

    @Data
    @AllArgsConstructor
    public static class ErrorDetail {
        private int row; // 엑셀 기준 1부터 시작
        private String reason;
    }

    public void addError(int row, String reason) {
        errors.add(new ErrorDetail(row, reason));
    }

    public void setResult(int total, int success) {
        this.total = total;
        this.success = success;
        this.failed = total - success;
    }
}