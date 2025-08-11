package com.example.memo.branch.dto;

import lombok.Data;

@Data
public class BranchNearbyRequest {
    private double latitude;   // 내 위도
    private double longitude;  // 내 경도
    private Double radiusKm;   // 검색 반경(km) - null이면 서버에서 기본 2.0
}
