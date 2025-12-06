package com.carecode.domain.chatbot.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    
    private String messageId;
    private String sessionId;
    private String sender;
    private String content;
    private LocalDateTime timestamp;
    private Boolean isRead;
} 