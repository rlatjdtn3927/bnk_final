package com.example.memo.couple.handler;

import java.util.Base64;

import org.springframework.stereotype.Component;

import com.example.memo.couple.service.FamilyDocService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UploadFamilyDocHandler implements TcpMessageHandler {

    private final FamilyDocService familyDocService;

    @Override
    public boolean supports(Command command) {
        return command == Command.UPLOAD_FAMILY_DOC;
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            Long linkId = data.get("linkId").asLong();
            String fileName = data.get("fileName").asText();
            String contentType = data.get("contentType").asText();
            byte[] fileData = Base64.getDecoder().decode(data.get("fileData").asText());

            String s3Key = familyDocService.uploadFile(linkId, fileName, contentType, fileData);

            ObjectNode response = JsonNodeFactory.instance.objectNode();
            response.put("status", "SUCCESS");
            response.put("s3Key", s3Key);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            ObjectNode response = JsonNodeFactory.instance.objectNode();
            response.put("status", "FAIL");
            response.put("error", e.getMessage());
            return response;
        }
    }
}
