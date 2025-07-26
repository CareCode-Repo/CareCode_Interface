package com.carecode.domain.chatbot.entity;

import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 챗봇 대화 메시지 엔티티
 * 사용자와 챗봇 간의 대화 기록을 저장
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String response;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntentType intentType;

    @Column
    private Double confidence;

    @Column
    private String sessionId;

    @Column
    private Boolean isHelpful;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    /**
     * 메시지 타입 (사용자/챗봇)
     */
    public enum MessageType {
        USER,       // 사용자 메시지
        BOT         // 챗봇 응답
    }

    /**
     * 의도 타입 (챗봇이 이해한 사용자 의도)
     */
    public enum IntentType {
        GREETING,           // 인사
        QUESTION,           // 질문
        COMPLAINT,          // 불만/문의
        THANKS,             // 감사
        GOODBYE,            // 작별인사
        HEALTH_INFO,        // 건강 정보
        POLICY_INFO,        // 정책 정보
        FACILITY_INFO,      // 시설 정보
        EDUCATION_INFO,     // 교육 정보
        UNKNOWN             // 알 수 없음
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
} 