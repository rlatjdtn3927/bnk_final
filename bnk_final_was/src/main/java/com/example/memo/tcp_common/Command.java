package com.example.memo.tcp_common;
/*여기 추가로 정의하세요*/
/*약속 커맨드 정의 시에 항상 도메인 이름 먼저 하고 언더바 뒤로는 알아서*/
public enum Command {
    USER_GET,
    USER_DELETE,
    EMBEDDING_CREATE, // 문서 청크 생성 및 임베딩 요청
    CHAT_ASK_QUESTION; // 질문에 대한 응답 요청
}