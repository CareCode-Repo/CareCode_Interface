package com.carecode.domain.chatbot.llm;

import com.carecode.domain.chatbot.rag.RetrievedContext;

import java.util.Optional;

/** 챗봇 응답 생성기. 구현체를 바꾸면 LLM 공급자를 교체할 수 있다. */
public interface ChatCompletionClient {

    boolean isAvailable();

    /** 검색된 근거를 바탕으로 답변을 생성한다. */
    Optional<String> generateReply(String userMessage, RetrievedContext context);
}
