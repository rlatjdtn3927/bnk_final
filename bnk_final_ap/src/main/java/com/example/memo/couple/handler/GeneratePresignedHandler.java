package com.example.memo.couple.handler;

import org.springframework.stereotype.Component;

import com.example.memo.couple.service.S3Service;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GeneratePresignedHandler implements TcpMessageHandler {

    private final S3Service s3Service;

    @Override
    public boolean supports(Command command) {
        return command == Command.PRESIGN_FAMILY_DOC;
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            String key = data.get("key").asText();
            int ttl = data.has("ttlSeconds") ? data.get("ttlSeconds").asInt() : 600;
            String url = s3Service.generatePresignedGetUrl(key, java.time.Duration.ofSeconds(ttl));

            ObjectNode res = JsonNodeFactory.instance.objectNode();
            res.put("status", "SUCCESS");
            res.put("url", url);
            return res;
        } catch (Exception e) {
            ObjectNode res = JsonNodeFactory.instance.objectNode();
            res.put("status", "FAIL");
            res.put("error", e.getMessage());
            return res;
        }
    }
}