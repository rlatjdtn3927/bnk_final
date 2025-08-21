package com.example.memo.admin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySeriesDto {
	private List<String> labels; // ["2025-01","2025-02",...]
    private List<Long> irp;      // 각 월 IRP 건수
    private List<Long> dc;       // 각 월 DC 건수
}
