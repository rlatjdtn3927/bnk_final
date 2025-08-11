package com.example.memo.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.ChatLog;
import com.example.memo.jpa.repository.ChatLogRepository;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 요구사항:
 * - "상품이름 + 알려줘" → 상품 정보
 * - "두 상품이름 + 비교해줘" → 비교
 * - "20대 대학생에게 tdf 상품 하나 추천" → 추천
 * - 나머지 → 일반 응답
 *
 * 흐름:
 * 질문 임베딩 → 캐시 유사도 검색 → 의도/파라미터 파싱 → VectorStore 검색 → 의도별 프롬프트 → 모델 호출 → 로그 저장
 */
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final VectorStore vectorStore;          // PgVectorStore와 동일 인터페이스
    private final ChatModel chatModel;              // OpenAiChatModel
    private final ChatLogRepository chatLogRepository;
    private final EmbeddingModel embeddingModel;    // OpenAiEmbeddingModel

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 캐시 유사 질문 허용 임계값 (너무 높으면 캐시 발동 빈도 ↓) */
    private static final double SIMILARITY_THRESHOLD = 0.99;

    /** 의도 분류 */
    private enum Intent {
        PRODUCT_INFO,        // "상품A 알려줘"
        PRODUCT_COMPARE,     // "상품A와 상품B 비교해줘"
        PRODUCT_RECOMMEND,   // "20대 대학생에게 tdf 하나 추천"
        GENERAL              // 일반 질문
    }

    /** 파싱 결과 DTO */
    private static class ParsedQuery {
        Intent intent;
        // info
        String productName;         // 단일 상품명
        // compare
        String productA;
        String productB;
        // recommend
        String persona;             // "20대 대학생" 등
        String category;            // "tdf", "펀드", "etf", "예금" 등
        // raw
        String originalQuestion;
    }

    /** 외부 호출 진입점 */
    public String getChatResponse(String question) {
        String trimmedQ = (question == null) ? "" : question.trim();
        if (trimmedQ.isEmpty()) {
            return "질문이 비어 있습니다. 알고 싶은 내용을 입력해 주세요.";
        }

        // 1) 질문 임베딩 → 캐시 유사 질문 검색 (비용 절감/속도↑)
        List<Double> qVec = embedToList(trimmedQ);
        Optional<ChatLog> cached = findSimilarCachedAnswer(qVec);
        if (cached.isPresent()) {
            ChatLog hit = cached.get();
            System.out.printf("🎯 캐시 히트 (%.2f): %s%n", SIMILARITY_THRESHOLD, hit.getQuestionText());
            return hit.getAnswer();
        }

        // 2) 질문 파싱 (의도/파라미터)
        ParsedQuery parsed = parseQuestion(trimmedQ);

        // 3) 문서 검색 쿼리 구성 → VectorStore 검색
        String searchQuery = buildSearchQuery(trimmedQ, parsed);
        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(searchQuery)
                        .topK(5)
                        .build()
        );
        String context = similarDocs.stream().map(Document::getText).collect(Collectors.joining("\n\n"));

        // 4) 의도별 프롬프트 구성 (문서 기반 원칙 준수)
        Prompt prompt = buildPrompt(parsed, context, trimmedQ);

        // 5) 모델 호출 (Spring AI 버전별 getText/getContent 차이를 안전하게 처리)
        ChatResponse response = chatModel.call(prompt);
        String answer = safeExtractText(response);

        // 6) 로그 저장
        saveChatLog(trimmedQ, qVec, answer, context);

        return answer;
    }

    // ==========================
    // 임베딩/캐시 관련 유틸
    // ==========================

    /**
     * 임베딩 결과를 List<Double>로 강제 변환
     * - Spring AI 버전에 따라 embed(String)의 반환타입이 List<Double>/float[]/double[] 등 다를 수 있음
     * - 또한 embed(List<String>) 시그니처만 있는 경우도 고려 (리플렉션 폴백)
     */
    @SuppressWarnings("unchecked")
    private List<Double> embedToList(String text) {
        try {
            // 1) 우선 embed(String) 시그니처를 리플렉션으로 시도 (컴파일타입 충돌 회피)
            try {
                Method m = embeddingModel.getClass().getMethod("embed", String.class);
                Object raw = m.invoke(embeddingModel, text);
                List<Double> coerced = coerceToDoubleList(raw);
                if (!coerced.isEmpty()) return coerced;
            } catch (NoSuchMethodException ignore) {
                // 다음 단계로 진행
            }

            // 2) 폴백: embed(List<String>) → List<List<Number>> 형태일 가능성
            try {
                Method m2 = embeddingModel.getClass().getMethod("embed", List.class);
                Object raw2 = m2.invoke(embeddingModel, Collections.singletonList(text));
                if (raw2 instanceof List) {
                    List<?> outer = (List<?>) raw2;
                    if (!outer.isEmpty()) {
                        return coerceToDoubleList(outer.get(0));
                    }
                }
            } catch (NoSuchMethodException ignore) {
                // 사용할 수 있는 embed가 없다면 빈 벡터
            }
        } catch (Exception e) {
            // 임베딩 실패 시 캐시 미사용
        }
        return Collections.emptyList();
    }

    /** 다양한 타입(List/float[]/double[]/문자열 등)을 Double 리스트로 변환 */
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

    /** 캐시에서 유사 질문을 코사인 유사도로 탐색 */
    private Optional<ChatLog> findSimilarCachedAnswer(List<Double> questionVector) {
        if (questionVector.isEmpty()) return Optional.empty();

        List<ChatLog> logs = chatLogRepository.findAll();
        ChatLog best = null;
        double bestScore = -1.0;

        for (ChatLog log : logs) {
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

    /** 코사인 유사도 계산 */
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
    // 파싱/검색/프롬프트
    // ==========================

    /** 질문을 의도/파라미터로 파싱 */
    private ParsedQuery parseQuestion(String q) {
        ParsedQuery pq = new ParsedQuery();
        pq.originalQuestion = q;
        String lower = q.toLowerCase(Locale.ROOT);

        // 1) 비교: "~ 비교"를 포함하고, 앞부분에서 두 상품 추출
        if (lower.contains("비교")) {
            String[] pair = extractTwoProductsForCompare(q);
            if (pair[0] != null && pair[1] != null) {
                pq.intent = Intent.PRODUCT_COMPARE;
                pq.productA = pair[0];
                pq.productB = pair[1];
                return pq;
            }
        }

        // 2) 추천
        if (lower.contains("추천")) {
            pq.intent = Intent.PRODUCT_RECOMMEND;
            pq.persona = extractPersona(q);     // 예: "20대 대학생", "30대 직장인"
            pq.category = extractCategory(q);   // 예: "tdf", "etf", "펀드", "예금"
            return pq;
        }

        // 3) 정보
        if (lower.contains("알려줘") || lower.contains("정보") || lower.contains("뭐야") || lower.contains("무엇") || lower.contains("머야")) {
            String name = extractSingleProductName(q);
            if (name != null && !name.isBlank()) {
                pq.intent = Intent.PRODUCT_INFO;
                pq.productName = name;
                return pq;
            }
        }

        // 4) 일반
        pq.intent = Intent.GENERAL;
        return pq;
    }

    /**
     * 비교용 두 상품 추출.
     * - 전략: "비교" 앞부분을 다양한 구분자(와/과/하고/및/랑/,/&// vs / 대)로 나눠 상위 2개 토큰을 사용.
     * - 한국어 조사/불용어는 간단 제거만 수행.
     */
    private String[] extractTwoProductsForCompare(String q) {
        String beforeCompare = q.split("비교")[0];            // "A 와 B" 등
        // 다양한 연결자를 , 로 통일
        String normalized = beforeCompare
                .replaceAll("(?i)\\s+vs\\s+", ",")
                .replace(" VS ", ",")
                .replace("/", ",")
                .replace("&", ",")
                .replace(" 와 ", ",")
                .replace(" 과 ", ",")
                .replace(" 하고 ", ",")
                .replace(" 및 ", ",")
                .replace("랑", ",")
                .replace(" 대 ", ",");

        // 쉼표 기준 분해 후 깨끗이 정리
        String[] raw = normalized.split(",");
        List<String> tokens = new ArrayList<>();
        for (String s : raw) {
            String t = s.replaceAll("(을|를|은|는|이|가)$", "").trim();
            if (!t.isBlank()) tokens.add(t);
        }

        if (tokens.size() >= 2) {
            return new String[]{tokens.get(0), tokens.get(1)};
        }
        return new String[]{null, null};
    }

    /** 단일 상품명 추출: "OOO 알려줘/정보/뭐야/무엇/에 대해" 등 형식에서 마커 단어 제거 */
    private String extractSingleProductName(String q) {
        String cleaned = q.replaceAll("(알려줘|정보|뭐야|무엇|머야|에 대해|에대한|설명해줘)", " ")
                          .replaceAll("\\s+", " ")
                          .trim();
        return (cleaned.length() < 2) ? null : cleaned;
    }

    /** 페르소나 간단 추출: "~에게/~한테/~용" 이전 토큰 또는 "20대|30대 + 대학생|직장인" 패턴 */
    private String extractPersona(String q) {
        var m1 = Pattern.compile("(.+?)(에게|한테|용)\\s*").matcher(q);
        if (m1.find()) {
            String p = m1.group(1).trim();
            if (!p.isBlank() && p.length() <= 20) return p;
        }
        var m2 = Pattern.compile("((10|20|30|40|50)대\\s*(대학생|직장인)?)").matcher(q);
        if (m2.find()) return m2.group(1).trim();
        return null;
    }

    /** 카테고리 추출: tdf/etf/펀드/예금/적금 */
    private String extractCategory(String q) {
        String lower = q.toLowerCase(Locale.ROOT);
        if (lower.contains("tdf")) return "tdf";
        if (lower.contains("etf")) return "etf";
        if (lower.contains("펀드")) return "펀드";
        if (lower.contains("예금")) return "예금";
        if (lower.contains("적금")) return "적금";
        return null;
    }

    /** 검색 쿼리 구성: 의도에 맞춰 키워드 보강 */
    private String buildSearchQuery(String original, ParsedQuery pq) {
        switch (pq.intent) {
            case PRODUCT_INFO:
                return (pq.productName + " " + original).trim();
            case PRODUCT_COMPARE:
                return (pq.productA + " " + pq.productB + " 비교 " + original).trim();
            case PRODUCT_RECOMMEND:
                String persona = pq.persona == null ? "" : pq.persona;
                String category = pq.category == null ? "" : pq.category;
                return (persona + " " + category + " 추천 " + original).trim();
            default:
                return original;
        }
    }

    /** 의도별 시스템 프롬프트 생성 (+ 문서 기반 안전장치 포함) */
    private Prompt buildPrompt(ParsedQuery pq, String context, String userQuestion) {
        String base = """
            당신은 금융 상품에 대한 전문 지식을 갖춘 친절한 챗봇입니다.
            반드시 아래 [문서 내용]만을 근거로 답하십시오.
            [문서 내용]에 없는 정보는 "문서에 해당 정보가 없어 답변할 수 없습니다."라고 명확히 말하세요.
            추측하거나 지어내지 마세요. 핵심만 간결하게, 표/불릿을 적절히 사용해 주세요.

            [문서 내용]
            %s
            """.formatted(context == null ? "" : context);

        String extra;
        switch (pq.intent) {
            case PRODUCT_INFO:
                extra = """
                    사용자의 의도는 '상품 정보'입니다.
                    요청된 상품명: %s
                    1) 상품의 핵심 개요
                    2) 주요 특징(수수료/위험/자산구성 등)
                    3) 적합한 투자자 유형
                    4) 문서 출처의 맥락
                    위 항목을 간결히 정리하세요.
                    """.formatted(nullToDash(pq.productName));
                break;

            case PRODUCT_COMPARE:
                extra = """
                    사용자의 의도는 '두 상품 비교'입니다.
                    비교 대상: %s  vs  %s
                    1) 유형/전략
                    2) 수수료(총보수)
                    3) 위험도/변동성 포인트
                    4) 운용사/기초지수(해당 시)
                    5) 어떤 상황에서 A를, 어떤 상황에서 B를 고려할지
                    표 형태로 핵심 비교 후, 한줄 결론을 제시하세요.
                    """.formatted(nullToDash(pq.productA), nullToDash(pq.productB));
                break;

            case PRODUCT_RECOMMEND:
                extra = """
                    사용자의 의도는 '상품 추천'입니다.
                    사용자 페르소나: %s
                    관심 카테고리: %s
                    1) 추천 1종만 제시
                    2) 추천 이유(문서 근거 2~3개)
                    3) 유의사항(위험, 수수료 등)
                    문서에 없는 내용은 절대 추가하지 마세요.
                    """.formatted(nullToDash(pq.persona), nullToDash(pq.category));
                break;

            default:
                // 일반 응답: 컨텍스트가 없으면 일반 대화형, 있으면 문서 범위 내에서만
                if (context == null || context.isBlank()) {
                    return new Prompt(List.of(
                            new SystemMessage("당신은 유용하고 친절한 일반 대화형 AI입니다. 상식선에서 성실히 답하세요."),
                            new UserMessage(userQuestion)
                    ));
                }
                extra = """
                    일반 질문이지만 문서 내용이 제공되었습니다.
                    반드시 문서 내용 안에서만 근거를 찾아 답하세요.
                    """;
        }

        List<Message> msgs = new ArrayList<>();
        msgs.add(new SystemMessage(base + "\n" + extra));
        // 문서가 비어있는데 문서 기반 의도면, UX상 고지 문구를 한 번 알려줌
        if ((context == null || context.isBlank())
                && (pq.intent == Intent.PRODUCT_INFO || pq.intent == Intent.PRODUCT_COMPARE || pq.intent == Intent.PRODUCT_RECOMMEND)) {
            msgs.add(new AssistantMessage("문서에 해당 내용이 없을 수 있어요. 가능한 범위에서 답을 시도합니다."));
        }
        msgs.add(new UserMessage(userQuestion));

        return new Prompt(msgs);
    }

    /** null/빈 문자열 → "-" */
    private String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    // ==========================
    // 모델 응답 텍스트 안전 추출
    // ==========================

    /**
     * Spring AI 버전에 따라 ChatResponse 구조가 조금씩 달라질 수 있어
     * - getResult().getOutput().getText()
     * - getResult().getOutput().getContent()
     * 등으로 달라질 수 있음 → 리플렉션으로 안전 추출
     */
    private String safeExtractText(ChatResponse response) {
        try {
            Object result = invokeGetter(response, "getResult");
            Object output = invokeGetter(result, "getOutput");

            // 1) getText()
            Object text = tryInvokeGetter(output, "getText");
            if (text instanceof String) return (String) text;

            // 2) getContent()
            Object content = tryInvokeGetter(output, "getContent");
            if (content instanceof String) return (String) content;

            // 3) toString()
            return String.valueOf(output);
        } catch (Exception e) {
            // 최후의 보루
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
}
