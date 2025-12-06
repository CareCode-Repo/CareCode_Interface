package com.carecode.domain.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 세션 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotSessionDtoResponse {
    private String sessionId;
    private String title;
    private String description;
    private String status;
    private Integer messageCount;
    private LocalDateTime lastActivityAt;
    private LocalDateTime createdAt;
}

