package com.example.memo.tcp_common;


/*여기 추가로 정의하세요*/
public enum Command {
    USER_GET,
    USER_DELETE,

    EMBEDDING_CREATE, // 문서 청크 생성 및 임베딩 요청
    CHAT_ASK_QUESTION, // 질문에 대한 응답 요청
    BRANCH_NEARBY; // 현재 위치 기준, 부산은행 지점 Top5

    
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