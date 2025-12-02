package com.carecode.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 기록 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotChatHistoryResponse {
    private String sessionId;
    private String userId;
    private List<ChatbotChatMessageResponse> messages;
    private LocalDateTime startTime;
    private LocalDateTime lastActivity;
    private String status;
}

