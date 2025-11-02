package com.carecode.domain.chatbot.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 챗봇 응답 DTO들
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotResponseDto {
    
    private Long messageId;
    private String response;
    private String intentType;
    private Double confidence;
    private String sessionId;
    private LocalDateTime timestamp;
    
    /**
     * 대화 기록 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatHistoryResponse {
        private Long messageId;
        private String message;
        private String response;
        private String messageType;
        private String intentType;
        private Double confidence;
        private String sessionId;
        private Boolean isHelpful;
        private LocalDateTime createdAt;
    }
    
    /**
     * 세션 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionResponse {
        private String sessionId;
        private String title;
        private String description;
        private String status;
        private Integer messageCount;
        private LocalDateTime lastActivityAt;
        private LocalDateTime createdAt;
    }
    
    /**
     * 피드백 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackResponse {
        private Long messageId;
        private Boolean isHelpful;
        private String message;
        private LocalDateTime updatedAt;
    }
    
    /**
     * 관련 정보 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedInfoDto {
        private String title;
        private String description;
        private String url;
        private String category;
    }
    
    /**
     * 지식 베이스 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KnowledgeBaseResponse {
        private String entryId;
        private String title;
        private String content;
        private String category;
        private Integer minAge;
        private Integer maxAge;
        private Double relevanceScore;
    }
    
    /**
     * 챗봇 통계 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatbotStatsResponse {
        private Integer totalSessions;
        private Integer activeSessions;
        private Double averageRating;
        private Map<String, Integer> categoryDistribution;
        private Map<String, Integer> ageGroupDistribution;
    }
} 