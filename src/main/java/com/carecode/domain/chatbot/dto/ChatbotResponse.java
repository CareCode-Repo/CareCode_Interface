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
 * 챗봇 관련 응답 DTO 통합 클래스
 */
public class ChatbotResponse {

    /**
     * 챗봇 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Chatbot {
        private String sessionId;
        private String userId;
        private String question;
        private String answer;
        private String confidence;
        private List<String> suggestions;
        private Map<String, Object> metadata;
        private LocalDateTime timestamp;
    }

    /**
     * 채팅 기록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatHistory {
        private String sessionId;
        private String userId;
        private List<ChatMessage> messages;
        private LocalDateTime startTime;
        private LocalDateTime lastActivity;
        private String status;
    }

    /**
     * 채팅 메시지 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatMessage {
        private Long id;
        private String sessionId;
        private String messageType; // USER, BOT
        private String content;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;
    }

    /**
     * 세션 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Session {
        private String sessionId;
        private String userId;
        private String topic;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime lastActivity;
        private int messageCount;
        private Map<String, Object> context;
    }

    /**
     * 피드백 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Feedback {
        private String sessionId;
        private String feedback;
        private Integer rating;
        private String category;
        private LocalDateTime timestamp;
        private String status;
    }

    /**
     * 지식베이스 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KnowledgeBase {
        private String id;
        private String title;
        private String content;
        private String category;
        private List<String> tags;
        private String source;
        private LocalDateTime lastUpdated;
    }

    /**
     * 챗봇 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatbotStats {
        private long totalSessions;
        private long totalMessages;
        private long activeUsers;
        private double averageSessionDuration;
        private double satisfactionRate;
        private List<String> popularTopics;
        private Map<String, Long> categoryDistribution;
    }

    /**
     * 관련 정보 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedInfo {
        private String title;
        private String description;
        private String url;
        private String type;
        private String category;
    }
}
