package com.example.memo.purchase.dto;

import lombok.AllArgsConstructor; 
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class PlannedItemDto {
  private Long id; // ← 없으면 추가
  private String category; 
  private String name; 
  private int ratio; 
  private long amount; 
  private String note;
  public PlannedItemDto(String c,String n,int r,long a,String note){ this.category=c; this.name=n; this.ratio=r; this.amount=a; this.note=note; }
}

