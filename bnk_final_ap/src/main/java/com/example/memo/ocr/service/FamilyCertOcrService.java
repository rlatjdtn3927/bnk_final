package com.example.memo.ocr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.memo.ocr.client.ClovaOcrClient;
import com.example.memo.ocr.dto.FamilyCertOcrRequest;
import com.example.memo.ocr.dto.FamilyCertOcrResponse;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class FamilyCertOcrService {

    private final ClovaOcrClient client;

    @Value("${family.ocr.template-id}")
    private String templateId;

    public FamilyCertOcrService(ClovaOcrClient client) {
        this.client = client;
    }

    public FamilyCertOcrResponse recognizeAndVerify(FamilyCertOcrRequest req) {
        // 1) CLOVA 호출 (Base64)
        JsonNode root = client.inferByBase64(
            req.base64Data(),
            (req.format() == null || req.format().isBlank()) ? "pdf" : req.format(),
            templateId
        );

        JsonNode fields = root.path("images").get(0).path("fields");

        // 2) 필드 파싱 (템플릿 필드명)
        String docType       = get(fields, "doc_type");
        String applicantName = get(fields, "applicant_name");
        String applicantRrn  = get(fields, "applicant_rrn");
        String spouseName    = get(fields, "spouse_name");
        String spouseRrn     = get(fields, "spouse_rrn");

        // 3) 성명 일치 여부만 검증
        boolean ok = true;
        String reason = "";
        if (!normalize(applicantName).equals(normalize(req.expectedApplicantName()))) {
            ok = false;
            reason = "본인 성명 불일치";
        }

        return new FamilyCertOcrResponse(
            docType, applicantName, applicantRrn, spouseName, spouseRrn, ok, reason
        );
    }

    /* ------ helpers ------ */

    private static String get(JsonNode fields, String name) {
        for (JsonNode f : fields) {
            if (name.equals(f.path("name").asText())) {
                return f.path("inferText").asText("");
            }
        }
        return "";
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toLowerCase();
    }
}