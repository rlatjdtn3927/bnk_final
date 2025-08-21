package com.example.memo.couple;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/link-admin")
@RequiredArgsConstructor
public class AdminLinkRestController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    /** 관리자 대기 목록(JSON) */
    @GetMapping(value = "/pending", produces = "application/json")
    public ResponseEntity<?> listPending() {
        TcpMessage msg = new TcpMessage(Command.ADMIN_SPOUSE_LINK_LIST, om.createObjectNode());
        JsonNode root = tcp.sendMessage(msg);
        return ResponseEntity.ok(root);
    }

    /** 관리자 승인/반려(JSON) */
    @PostMapping(value = "/decide", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> decide(@RequestBody JsonNode body) {
        TcpMessage msg = new TcpMessage(Command.ADMIN_SPOUSE_LINK_DECIDE, body);
        JsonNode root = tcp.sendMessage(msg);
        return ResponseEntity.ok(root);
    }
}