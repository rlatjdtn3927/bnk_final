package com.example.memo.couple.ocr.service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.memo.couple.ocr.dto.SpouseOcrRequest;
import com.example.memo.couple.ocr.dto.SpouseOcrResponse;
import com.example.memo.couple.ocr.dto.SpouseOcrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import static com.example.memo.couple.ocr.OcrTextUtil.*;


@Service
@RequiredArgsConstructor
public class ClovaSpouseOcrService implements SpouseOcrService {

    @Value("${clova.ocr.url}")        private String ocrUrl;
    @Value("${clova.ocr.secret}")     private String secret;
    @Value("${family.ocr.template-id}") private String templateId;

    private final ObjectMapper om = new ObjectMapper();
    private final RestTemplate restTemplate = buildRestTemplate();

    @Override
    public SpouseOcrResponse parse(SpouseOcrRequest req) {
        SpouseOcrResponse out = new SpouseOcrResponse();
        try {
            String base64 = Objects.requireNonNull(req.getBase64(), "base64 is required");

            // ✅ 무조건 PDF만 허용
            if (!isPdf(req.getFilename(), req.getContentType())) {
                out.setOk(false);
                out.setReason("PDF만 업로드할 수 있습니다.");
                return out;
            }

            Map<String, Object> image = Map.of(
                    "format", "pdf",            // 고정
                    "name",   "family-cert",
                    "data",   base64
            );

            Map<String, Object> body = new HashMap<>();
            body.put("images", List.of(image));
            body.put("requestId", UUID.randomUUID().toString());
            body.put("version", "V2");
            body.put("timestamp", System.currentTimeMillis());
            body.put("templateIds", List.of(templateId));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", secret);

            ResponseEntity<String> res = restTemplate.exchange(
                    ocrUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("CLOVA OCR HTTP " + res.getStatusCodeValue());
            }

            JsonNode root   = om.readTree(res.getBody());
            JsonNode fields = root.path("images").get(0).path("fields");

            // 필드 추출(템플릿 필드명은 운영 템플릿에 맞게)
            String docType          = get(fields, "doc_type");
            String applicantNameRaw = get(fields, "applicant_name");
            String applicantBirthF  = get(fields, "applicant_birth");
            String applicantRrn     = get(fields, "applicant_rrn");
            String spouseNameRaw    = get(fields, "spouse_name");
            String spouseBirthF     = get(fields, "spouse_birth");
            String spouseRrn        = get(fields, "spouse_rrn");

            // ✅ 이름 전처리(한자/괄호 제거, 공백 정리)
            String applicantName = tidyNameForDisplay(applicantNameRaw);
            String spouseName    = tidyNameForDisplay(spouseNameRaw);

            // ✅ 생년월일 표준화: 필드 없으면 주민번호로 파생(1/2=1900, 3/4=2000)
            String applicantBirth = hasText(applicantBirthF) ? normalizeBirth(applicantBirthF) : birthFromRRN(applicantRrn);
            String spouseBirth    = hasText(spouseBirthF)    ? normalizeBirth(spouseBirthF)    : birthFromRRN(spouseRrn);

            boolean relation = hasText(spouseName) && (hasText(spouseBirth) || hasText(spouseRrn));
            boolean ok       = hasText(applicantName) && hasText(spouseName);

            out.setOk(ok);
            out.setReason(null);
            out.setDocType(docType);
            out.setApplicantName(applicantName);
            out.setApplicantBirth(applicantBirth);
            out.setSpouseName(spouseName);
            out.setSpouseBirth(spouseBirth);
            out.setSpouseRelation(relation);
            return out;

        } catch (Exception e) {
            out.setOk(false);
            out.setReason(e.getMessage());
            return out;
        }
    }

    /* ================= helpers ================= */

    private static RestTemplate buildRestTemplate() {
        var f = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(30_000);
        return new RestTemplate(f);
    }

    private boolean isPdf(String filename, String contentType) {
        boolean byName = filename != null && filename.toLowerCase().endsWith(".pdf");
        boolean byCt   = contentType != null && contentType.toLowerCase().contains("pdf");
        return byName || byCt;
    }

    private boolean hasText(String s) { return s != null && !s.isBlank(); }

    private String get(JsonNode fields, String name) {
        if (fields == null || !fields.isArray()) return "";
        for (JsonNode f : fields) {
            if (name.equals(f.path("name").asText())) {
                return f.path("inferText").asText("");
            }
        }
        return "";
    }
}