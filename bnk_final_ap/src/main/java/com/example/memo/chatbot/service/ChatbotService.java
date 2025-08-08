package com.example.memo.chatbot.service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.ChatLog;
import com.example.memo.jpa.repository.ChatLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatbotService {

    private final VectorStore       vectorStore;      // PgVectorStore
    private final ChatModel         chatModel;        // OpenAiChatModel
    private final ChatLogRepository chatLogRepository;

    public String getChatResponse(String question) throws JsonProcessingException {

        /* 1) Top-k 유사 청크 검색 ---------------------------------------- */
    	
        SearchRequest searchRequest = SearchRequest.builder()   // 0.8.x
                                                   .query(question)
                                                   .topK(5)
                                                   .build();

        List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);

        /* 2) 컨텍스트 문자열 만들기 -------------------------------------- */
        
        String context = similarDocs.stream()
                                    .map(Document::getText)
                                    .collect(Collectors.joining("\n\n"));

        /* 3) GPT 프롬프트 구성 ----------------------------------------- */

        String sysMsg = """
                당신은 문서 기반 챗봇입니다. 아래 문서 내용을 참고해 답하세요.
                문서 내용:
                %s
                """.formatted(context);

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(sysMsg),
                        new UserMessage(question)   // ⬅ 사용자 질문
                )
        );

        /* 4) GPT 호출 -------------------------------------------------- */

        // ChatResponse → ChatGeneration → ChatMessage → content
        String answer = chatModel.call(prompt)
                                 .getResult()      // ChatGeneration
                                 .getOutput()     // ChatMessage
                                 .getText();    // 답변 String

        /* 5) 대화 로그 저장 -------------------------------------------- */

        chatLogRepository.save(ChatLog.builder()
                .questionText(question)
                .questionEmbed("TODO: 벡터 저장")   // 추후 구현
                .answer(answer)
                .context(context)
                .createdAt(new Date())
                .build());

        return answer;
    }
}
