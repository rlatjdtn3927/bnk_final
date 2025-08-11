package com.example.memo.tcp_common;


/*여기 추가로 정의하세요*/
public enum Command {
    USER_GET,
    USER_DELETE,
    
    /*IRP가입관련*/
    IRP_JOIN_CREATE_DRAFT,      // step1: 초안 생성
    IRP_JOIN_UPDATE_CONTRACT,   // step3-2: 계약정보 업데이트
    IRP_JOIN_UPDATE_PRODUCT,    // step4: 상품 저장
    IRP_JOIN_SAVE,
    IRP_JOIN_GET,
    IRP_JOIN_TAX_PURPOSE, 
    IRP_JOIN_RETIRED_PURPOSE, 
    IRP_JOIN_COMPLETE,			// 계좌개설/완료
}