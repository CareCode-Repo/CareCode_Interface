package com.carecode.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 채팅 메시지 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotChatMessageResponse {
    private Long id;
    private String sessionId;
    private String messageType; // USER, BOT
    private String content;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}

