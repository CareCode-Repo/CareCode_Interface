package com.carecode.domain.chatbot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 피드백 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotFeedbackDtoRequest {
    private String sessionId;
    private String responseId;
    private Integer rating;
    private String feedback;
    private String improvement;
}

