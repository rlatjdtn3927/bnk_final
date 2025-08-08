package com.example.memo.tcp_common;


/*여기 추가로 정의하세요*/
public enum Command {
    USER_GET,
    USER_DELETE,
    
    /*IRP가입관련*/
    IRP_JOIN_SAVE,
    IRP_JOIN_GET,
    IRP_JOIN_TAX_PURPOSE, 
    IRP_JOIN_RETIRED_PURPOSE, 
    IRP_JOIN_TRANSFER_PURPOSE,
    IRP_JOIN_COMPLETE,
}