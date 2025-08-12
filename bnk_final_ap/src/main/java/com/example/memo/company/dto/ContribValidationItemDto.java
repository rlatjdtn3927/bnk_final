package com.example.memo.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class ContribValidationItemDto {
    private Long itemId;
    private Long amount;
    private String validationStatus; // SUCCESS / FAIL
    private String errorMessage;
    private DcMemberLite dcMember;   // { name }
}