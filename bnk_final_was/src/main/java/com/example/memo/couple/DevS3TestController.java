package com.example.memo.couple;

import java.util.Base64;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
@RequestMapping("/dev/s3")
public class DevS3TestController {

    private static final String VIEW = "couple/dev_s3_test"; // templates/couple/dev_s3_test.html

    private final TcpClientService tcpClientService;

    /** 업로드 폼 */
    @GetMapping
    public String form(Model model) {
        return VIEW;
    }

    /** 업로드 실행 → AP 업로드 → presigned URL 요청 → 결과 화면에 표시 */
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam("linkId") Long linkId,
                         Model model) {
        try {
            // 1) 업로드
            ObjectNode data = JsonNodeFactory.instance.objectNode();
            data.put("linkId", linkId);
            data.put("fileName", file.getOriginalFilename());
            data.put("contentType", file.getContentType());
            data.put("fileData", Base64.getEncoder().encodeToString(file.getBytes()));

            TcpMessage uploadMsg = new TcpMessage(Command.UPLOAD_FAMILY_DOC, data);
            JsonNode uploadRes = tcpClientService.sendMessage(uploadMsg);

            if (uploadRes == null || !"SUCCESS".equals(uploadRes.path("status").asText())) {
                model.addAttribute("error", uploadRes != null ? uploadRes.toString() : "null response");
                return VIEW;
            }

            String s3Key = uploadRes.path("s3Key").asText();

            // 2) presigned URL 요청
            ObjectNode presignData = JsonNodeFactory.instance.objectNode();
            presignData.put("key", s3Key);
            presignData.put("ttlSeconds", 600); // 10분

            TcpMessage presignMsg = new TcpMessage(Command.PRESIGN_FAMILY_DOC, presignData);
            JsonNode presignRes = tcpClientService.sendMessage(presignMsg);

            String url = presignRes != null ? presignRes.path("url").asText(null) : null;

            model.addAttribute("linkId", linkId);
            model.addAttribute("s3Key", s3Key);
            model.addAttribute("presignedUrl", url);

            return VIEW;

        } catch (Exception e) {
            model.addAttribute("error", "예외 발생: " + e.getMessage());
            return VIEW;
        }
    }
}