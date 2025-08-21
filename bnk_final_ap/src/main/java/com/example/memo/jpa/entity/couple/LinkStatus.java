package com.example.memo.jpa.entity.couple;

public enum LinkStatus {
 APPLIED,          // 신청 접수(OCR 인증 대기)
 PENDING_ADMIN,    // OCR 실패 → 관리자 검토 대기
 PENDING_SPOUSE,   // 배우자 수락 대기(OCR 인증 완료)
 LINKED,           // 연동 완료
 REJECTED_ADMIN,   // 관리자 반려
 REJECTED_SPOUSE,  // 배우자 거절
 UNLINKED          // 연동 해지
}
