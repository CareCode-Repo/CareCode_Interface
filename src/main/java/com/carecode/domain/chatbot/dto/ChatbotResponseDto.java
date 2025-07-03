package com.carecode.domain.chatbot.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 챗봇 응답 DTO들
 */
public class ChatbotResponseDto {
    
    /**
     * 챗봇 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatbotResponse {
        private String response;
        private String category;
        private Double confidence;
        private List<RelatedInfoDto> relatedInfo;
        private List<String> suggestedQuestions;
        private List<String> followUpQuestions;
    }
    
    /**
     * 채팅 세션 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatSessionResponse {
        private String sessionId;
        private String userId;
        private UserProfileDto profile;
        private String status;
        private String startTime;
        private List<String> initialQuestions;
    }
    
    /**
     * 사용자 프로필 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserProfileDto {
        private String userId;
        private Integer childAge;
        private String childGender;
        private String parentType;
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