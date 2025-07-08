package com.carecode.domain.chatbot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 엔티티
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "message_type")
    private String messageType; // USER, BOT
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "is_read")
    private Boolean isRead;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
    }
} 