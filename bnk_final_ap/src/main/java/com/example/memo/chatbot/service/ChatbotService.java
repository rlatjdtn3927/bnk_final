// src/main/java/com/example/memo/chatbot/service/ChatbotService.java
package com.example.memo.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.chatbot.ChatLog;
import com.example.memo.jpa.repository.chatbot.ChatLogRepository;

import org.postgresql.util.PGobject;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    // 🔄 VectorStore 제거 —> 직접 pgvector 질의
    private final JdbcTemplate pgJdbcTemplate;   // @Qualifier("pgJdbcTemplate") 로 주입
    private final ChatModel chatModel;
    private final ChatLogRepository chatLogRepository;
    private final EmbeddingModel embeddingModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double SIMILARITY_THRESHOLD = 0.99; // 캐시 유사도 기준
    private static final int TOP_K = 5;                      // 유사 문서 개수

    // ✅ “무응답” 메시지 상수 & 헬퍼
    private static final String NO_INFO_MSG = "문서에 해당 정보가 없어 답변할 수 없습니다.";

    private boolean isNoInfoAnswer(String s) {
        if (s == null) return false;
        String t = s.replace("\"", "").trim();
        return t.equalsIgnoreCase(NO_INFO_MSG);
    }

    /** 외부 호출 진입점 */
    public String getChatResponse(String question) {
        String trimmedQ = (question == null) ? "" : question.trim();
        if (trimmedQ.isEmpty()) {
            return "질문이 비어 있습니다. 알고 싶은 내용을 입력해 주세요.";
        }

        // 1) 질문 임베딩 → 캐시 유사 질문 검색 (단, 무응답 레코드는 캐시 제외)
        List<Double> qVec = embedToList(trimmedQ);
        Optional<ChatLog> cached = findSimilarCachedAnswer(qVec);
        if (cached.isPresent()) {
            ChatLog hit = cached.get();
            System.out.printf("🎯 캐시 히트 (≥%.2f): %s%n", SIMILARITY_THRESHOLD, hit.getQuestionText());
            return hit.getAnswer();
        }

        // 2) pgvector에서 코사인 거리로 TOP_K 검색
        List<Document> similarDocs = querySimilarDocs(trimmedQ, TOP_K);
        String context = similarDocs.stream().map(Document::getText).collect(Collectors.joining("\n\n"));

        // ⚠️ 컨텍스트가 비면 모델 호출/저장 모두 스킵하고 바로 무응답 반환
        if (context == null || context.isBlank()) {
            return NO_INFO_MSG;
        }

        // 3) 프롬프트 생성
        Prompt prompt = buildPrompt(context, trimmedQ);

        // 4) 모델 호출
        ChatResponse response = chatModel.call(prompt);
        String answer = safeExtractText(response);

        // 5) 로그 저장 (✅ 무응답이면 저장하지 않음)
        if (!isNoInfoAnswer(answer)) {
            saveChatLog(trimmedQ, qVec, answer, context);
        }

        return answer;
    }

    // ==========================
    // pgvector 유사도 검색 (코사인)
    // ==========================
    private List<Document> querySimilarDocs(String question, int k) {
        // 질문 임베딩 (배치 1건)
        List<?> vecs = embeddingModel.embed(Collections.singletonList(question));
        if (vecs == null || vecs.isEmpty()) return Collections.emptyList();
        Object vec = vecs.get(0);

        // 벡터 리터럴 생성: [0.12,0.34,...]
        String vectorLiteral = toVectorLiteral(vec);

        // 코사인 거리 ORDER BY (작을수록 유사) — pgvector 연산자: <=>
        final String sql = """
            SELECT id, content, metadata
            FROM embedding
            ORDER BY embedding <=> ?    -- cosine distance
            LIMIT ?
            """;

        PGobject vectorObject = new PGobject();
        try {
            vectorObject.setType("vector");
            vectorObject.setValue(vectorLiteral);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set vector parameter", e);
        }

        return pgJdbcTemplate.query(sql,
                ps -> {
                    ps.setObject(1, vectorObject);
                    ps.setInt(2, k);
                },
                (rs, rowNum) -> mapRowToDocument(rs)
        );
    }

    private Document mapRowToDocument(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String content = rs.getString("content");
        String metadataJson = rs.getString("metadata");

        Map<String, Object> metadata = new HashMap<>();
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                metadata = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignore) {}
        }
        return new Document(id, content, metadata);
    }

    // ==========================
    // 임베딩/캐시 관련
    // ==========================
    @SuppressWarnings("unchecked")
    private List<Double> embedToList(String text) {
        try {
            // (1) embed(String) 대응
            try {
                Method m = embeddingModel.getClass().getMethod("embed", String.class);
                Object raw = m.invoke(embeddingModel, text);
                List<Double> coerced = coerceToDoubleList(raw);
                if (!coerced.isEmpty()) return coerced;
            } catch (NoSuchMethodException ignore) {}

            // (2) embed(List<String>) 대응
            try {
                Method m2 = embeddingModel.getClass().getMethod("embed", List.class);
                Object raw2 = m2.invoke(embeddingModel, Collections.singletonList(text));
                if (raw2 instanceof List) {
                    List<?> outer = (List<?>) raw2;
                    if (!outer.isEmpty()) {
                        return coerceToDoubleList(outer.get(0));
                    }
                }
            } catch (NoSuchMethodException ignore) {}
        } catch (Exception e) {
            // ignore
        }
        return Collections.emptyList();
    }

    private List<Double> coerceToDoubleList(Object raw) {
        if (raw == null) return Collections.emptyList();

        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            List<Double> out = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o instanceof Number) {
                    out.add(((Number) o).doubleValue());
                } else if (o instanceof String) {
                    try { out.add(Double.parseDouble((String) o)); } catch (NumberFormatException ignore) {}
                }
            }
            return out;
        }
        if (raw instanceof float[]) {
            float[] fa = (float[]) raw;
            List<Double> out = new ArrayList<>(fa.length);
            for (float f : fa) out.add((double) f);
            return out;
        }
        if (raw instanceof double[]) {
            double[] da = (double[]) raw;
            List<Double> out = new ArrayList<>(da.length);
            for (double d : da) out.add(d);
            return out;
        }
        return Collections.emptyList();
    }

    private Optional<ChatLog> findSimilarCachedAnswer(List<Double> questionVector) {
        if (questionVector.isEmpty()) return Optional.empty();

        List<ChatLog> logs = chatLogRepository.findAll();
        ChatLog best = null;
        double bestScore = -1.0;

        for (ChatLog log : logs) {
            // ✅ 무응답 레코드는 캐시 후보에서 제외
            if (isNoInfoAnswer(log.getAnswer())) continue;

            List<Double> existingVector = parseJsonToVector(log.getQuestionEmbed());
            if (existingVector.isEmpty() || existingVector.size() != questionVector.size()) continue;

            double score = cosineSimilarity(questionVector, existingVector);
            if (score > bestScore) {
                bestScore = score;
                best = log;
            }
        }
        return (best != null && bestScore >= SIMILARITY_THRESHOLD) ? Optional.of(best) : Optional.empty();
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1.size() != v2.size() || v1.isEmpty()) return 0.0;
        double dot = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            double a = v1.get(i);
            double b = v2.get(i);
            dot += a * b;
            norm1 += a * a;
            norm2 += b * b;
        }
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private String convertEmbeddingToJson(List<Double> embedding) {
        try { return objectMapper.writeValueAsString(embedding); }
        catch (Exception e) { throw new RuntimeException("임베딩 벡터 JSON 변환 실패", e); }
    }

    private List<Double> parseJsonToVector(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    // ==========================
    // 프롬프트 생성
    // ==========================
    private Prompt buildPrompt(String context, String userQuestion) {
        if (context == null || context.isBlank()) {
            return new Prompt(List.of(
                    new SystemMessage("당신은 유용하고 친절한 일반 대화형 AI입니다. 상식선에서 성실히 답하세요."),
                    new UserMessage(userQuestion)
            ));
        }

        String base = """
            당신은 금융 상품에 대한 전문 지식을 갖춘 친절한 챗봇입니다.
            반드시 아래 [문서 내용]만을 근거로 답하십시오.
            [문서 내용]에 없는 정보는 "문서에 해당 정보가 없어 답변할 수 없습니다."라고 명확히 말하세요.
            추측하거나 지어내지 마세요.
            
            [문서 내용]
            %s
            """.formatted(context);

        List<Message> msgs = new ArrayList<>();
        msgs.add(new SystemMessage(base));
        msgs.add(new UserMessage(userQuestion));
        return new Prompt(msgs);
    }

    // ==========================
    // 응답 추출
    // ==========================
    private String safeExtractText(ChatResponse response) {
        try {
            Object result = invokeGetter(response, "getResult");
            Object output = invokeGetter(result, "getOutput");

            Object text = tryInvokeGetter(output, "getText");
            if (text instanceof String) return (String) text;

            Object content = tryInvokeGetter(output, "getContent");
            if (content instanceof String) return (String) content;

            return String.valueOf(output);
        } catch (Exception e) {
            return String.valueOf(response);
        }
    }

    private Object tryInvokeGetter(Object target, String method) {
        try { return invokeGetter(target, method); }
        catch (Exception e) { return null; }
    }

    private Object invokeGetter(Object target, String method) throws Exception {
        Method m = target.getClass().getMethod(method);
        return m.invoke(target);
    }

    // ==========================
    // 로그 저장
    // ==========================
    @Transactional
    public void saveChatLog(String question, List<Double> questionVector, String answer, String context) {
        String questionEmbedJson = convertEmbeddingToJson(questionVector);
        chatLogRepository.save(ChatLog.builder()
                .questionText(question)
                .questionEmbed(questionEmbedJson)
                .answer(answer)
                .context(context == null ? "" : context)
                .createdAt(new Date())
                .build());
    }

    // ==========================
    // 유틸: pgvector 리터럴 변환
    // ==========================
    private String toVectorLiteral(Object vec) {
        StringBuilder sb = new StringBuilder("[]");
        if (vec == null) return "[]";
        sb.setLength(1); // '['

        if (vec instanceof double[] da) {
            for (int i = 0; i < da.length; i++) { if (i>0) sb.append(','); sb.append(trim(da[i])); }
        } else if (vec instanceof float[] fa) {
            for (int i = 0; i < fa.length; i++) { if (i>0) sb.append(','); sb.append(trim(fa[i])); }
        } else if (vec instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                if (i>0) sb.append(',');
                Object o = list.get(i);
                double d = (o instanceof Number) ? ((Number) o).doubleValue() : Double.parseDouble(o.toString());
                sb.append(trim(d));
            }
        } else {
            throw new IllegalArgumentException("Unknown embedding vector type: " + vec);
        }
        return sb.append(']').toString();
    }

    private String trim(double v) {
        String s = Double.toString(v);
        if (s.indexOf('E') >= 0 || s.indexOf('e') >= 0) return s;
        if (s.indexOf('.') < 0) return s;
        int end = s.length();
        while (end > 0 && s.charAt(end-1) == '0') end--;
        if (end > 0 && s.charAt(end-1) == '.') end--;
        return s.substring(0, end);
    }
}
