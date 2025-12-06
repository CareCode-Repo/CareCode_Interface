package com.carecode.domain.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 세션 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotSessionResponse {
    private String sessionId;
    private String userId;
    private String topic;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime lastActivity;
    private int messageCount;
    private Map<String, Object> context;
}

