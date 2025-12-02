package com.carecode.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 피드백 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotFeedbackResponse {
    private String sessionId;
    private String feedback;
    private Integer rating;
    private String category;
    private LocalDateTime timestamp;
    private String status;
}

