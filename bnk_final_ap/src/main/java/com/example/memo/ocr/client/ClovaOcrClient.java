package com.example.memo.ocr.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ClovaOcrClient {

    @Value("${clova.ocr.url}")
    private String ocrUrl;

    @Value("${clova.ocr.secret}")
    private String secret;

    private final ObjectMapper om = new ObjectMapper();
    private final RestTemplate rest = new RestTemplate();

    /**
     * Base64 데이터를 CLOVA 템플릿 OCR로 전송
     */
    public JsonNode inferByBase64(String base64Data, String format, String templateId) {
        try {
            Map<String, Object> image = Map.of(
                "format", format,                 // "pdf"
                "name", "family-cert",
                "data", base64Data                // Base64 본문
            );

            Map<String, Object> body = Map.of(
                "images", List.of(image),
                "requestId", UUID.randomUUID().toString(),
                "version", "V2",
                "timestamp", System.currentTimeMillis(),
                "templateIds", List.of(templateId)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", secret);

            HttpEntity<Map<String,Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<String> res = rest.postForEntity(ocrUrl, req, String.class);

            if (res.getStatusCode() != HttpStatus.OK) {
                throw new IllegalStateException("CLOVA OCR error: " + res.getStatusCodeValue() + " / " + res.getBody());
            }
            return om.readTree(res.getBody());

        } catch (Exception e) {
            throw new RuntimeException("CLOVA OCR request failed", e);
        }
    }
}