package com.example.memo.couple;

import java.io.IOException;
import java.util.Base64;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/couple")
@RequiredArgsConstructor
public class FamilyDocController {

    private final TcpClientService tcpClientService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam("linkId") Long linkId) throws IOException {

        // 1. JSON data 구성
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put("linkId", linkId);
        data.put("fileName", file.getOriginalFilename());
        data.put("contentType", file.getContentType());
        data.put("fileData", Base64.getEncoder().encodeToString(file.getBytes()));

        // 2. TcpMessage 객체 생성
        TcpMessage message = new TcpMessage(Command.UPLOAD_FAMILY_DOC, data);

        // 3. TCP 전송 후 응답 수신
        JsonNode response = tcpClientService.sendMessage(message);

        return ResponseEntity.ok(response != null ? response.toString() : "FAIL");
    }
}
