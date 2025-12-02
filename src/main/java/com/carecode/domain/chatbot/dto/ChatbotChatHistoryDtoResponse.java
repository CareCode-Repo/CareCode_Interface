package com.carecode.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 대화 기록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotChatHistoryDtoResponse {
    private Long messageId;
    private String message;
    private String response;
    private String messageType;
    private String intentType;
    private Double confidence;
    private String sessionId;
    private Boolean isHelpful;
    private LocalDateTime createdAt;
}

