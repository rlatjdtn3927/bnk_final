package com.example.memo.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.memo.jpa.entity.ChatLog;
import com.example.memo.jpa.repository.ChatLogRepository;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final ChatLogRepository chatLogRepository;
    private final EmbeddingModel embeddingModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final double SIMILARITY_THRESHOLD = 0.9;

    private String convertEmbeddingToJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new RuntimeException("임베딩 벡터 JSON 변환 실패", e);
        }
    }

    private List<Double> parseJsonToVector(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public String getChatResponse(String question) {
        // ✅ 1. 질문 임베딩 및 유사 질문 탐색 (캐시처럼 사용)
    	float[] floatVector = embeddingModel.embed(question);
    	List<Double> questionVector = new ArrayList<>();
    	for (float f : floatVector) {
    	    questionVector.add((double) f);
    	}

        List<ChatLog> logs = chatLogRepository.findAll();
        for (ChatLog log : logs) {
            List<Double> existingVector = parseJsonToVector(log.getQuestionEmbed());
            if (existingVector.isEmpty()) continue;

            double similarity = cosineSimilarity(questionVector, existingVector);
            if (similarity >= SIMILARITY_THRESHOLD) {
                System.out.printf("🎯 유사 질문 발견 (%.4f): %s%n", similarity, log.getQuestionText());
                return log.getAnswer();
            }
        }

        // ✅ 2. 문서 검색
        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(5).build()
        );
        String context = similarDocs.stream()
                                  .map(Document::getText)
                                  .collect(Collectors.joining("\n\n"));

        if (context.isBlank()) {
            return "죄송합니다. 현재 질문에 대한 문서 기반 정보가 없어 답변을 제공할 수 없습니다.";
        }

        // ✅ 3. 질문 유형 판단 & 시스템 메시지 생성
        String sysPrompt = buildSystemPrompt(question, context);

        // ✅ 4. GPT 프롬프트 구성 및 호출
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(sysPrompt),
                new UserMessage(question)
        ));
        String answer = chatModel.call(prompt).getResult().getOutput().getText();

        // ✅ 5. 로그 저장
        saveChatLog(question, questionVector, answer, context);

        return answer;
    }

    @Transactional
    public void saveChatLog(String question, List<Double> questionVector, String answer, String context) {
        String questionEmbedJson = convertEmbeddingToJson(questionVector);
        chatLogRepository.save(ChatLog.builder()
                .questionText(question)
                .questionEmbed(questionEmbedJson)
                .answer(answer)
                .context(context)
                .createdAt(new Date())
                .build());
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1.size() != v2.size()) return 0.0;
        double dot = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dot += v1.get(i) * v2.get(i);
            norm1 += Math.pow(v1.get(i), 2);
            norm2 += Math.pow(v2.get(i), 2);
        }
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private String buildSystemPrompt(String question, String context) {
        String lowerQ = question.toLowerCase();

        if (lowerQ.contains("비교")) {
            return """
                당신은 금융 전문가입니다.
                아래 두 금융 상품을 문서 기반으로 객관적으로 비교하세요.
                문서 내용:
                %s
                """.formatted(context);

        } else if (lowerQ.contains("추천")) {
            return """
                당신은 금융 상품 추천 전문가입니다.
                사용자 상황과 문서 기반 정보를 고려해 적절한 상품을 추천하세요.
                문서 내용:
                %s
                """.formatted(context);

        } else if (lowerQ.contains("알려줘") || lowerQ.contains("정보") || lowerQ.contains("무엇")) {
            return """
                당신은 금융 설명 챗봇입니다.
                아래 문서 내용을 참고하여 사용자의 질문에 친절하고 정확하게 답변하세요.
                문서 내용:
                %s
                """.formatted(context);

        } else {
            // 문서 기반 질문이 아닌 경우 (일반 GPT 질문용)
            return "당신은 유용하고 친절한 AI 챗봇입니다. 사용자의 질문에 성실하게 답변하세요.";
        }
    }
}