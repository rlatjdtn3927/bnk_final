// src/main/java/com/example/memo/purchase/handler/DocumentHandler.java
package com.example.memo.purchase.handler;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.service.DocumentService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/** DOCUMENT_* : 상품별 서류(FundDocument/PrincipalDocument) */
@Component
@RequiredArgsConstructor
public class DocumentHandler implements TcpMessageHandler {

    private final DocumentService service;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("DOCUMENT_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        switch (command) {
            case DOCUMENT_LIST:
                return service.listDocuments(data);
            default:
                return "알 수 없는 DOCUMENT 명령: " + command.name();
        }
    }
}
