package com.carecode.domain.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 챗봇 메시지 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotMessageResponse {
    private Long messageId;
    private String response;
    private String intentType;
    private Double confidence;
    private String sessionId;
    private LocalDateTime timestamp;
}

