package com.carecode.domain.chatbot.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 챗봇 요청 DTO들
 */
public class ChatbotRequestDto {
    
    /**
     * 기본 챗봇 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatbotRequest {
        private String userId;
        private String message;
        private Integer childAge;
        private String context;
    }
    
    /**
     * 질문 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AskQuestionRequest {
        private String question;
        private Integer childAge;
        private String context;
    }
    
    /**
     * 세션 시작 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StartSessionRequest {
        private String userId;
        private Integer childAge;
        private String childGender;
        private String parentType;
        private String concerns;
    }
    
    /**
     * 맥락 기반 질문 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContextualQuestionRequest {
        private String question;
        private List<ChatMessageDto> conversationHistory;
    }
    
    /**
     * 피드백 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackRequest {
        private String sessionId;
        private String responseId;
        private Integer rating;
        private String feedback;
        private String improvement;
    }
} 