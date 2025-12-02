package com.carecode.domain.chatbot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 맥락 기반 질문 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotContextualQuestionDtoRequest {
    private String question;
    private List<ChatMessageDto> conversationHistory;
}

