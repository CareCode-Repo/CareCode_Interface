package com.carecode.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 챗봇 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotInfoResponse {
    private String sessionId;
    private String userId;
    private String question;
    private String answer;
    private String confidence;
    private List<String> suggestions;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
}

