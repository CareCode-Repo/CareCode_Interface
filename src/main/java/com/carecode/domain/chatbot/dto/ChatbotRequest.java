package com.carecode.domain.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 챗봇 관련 요청 DTO 통합 클래스
 */
public class ChatbotRequest {

    /**
     * 챗봇 질문 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AskQuestion {
        @NotBlank(message = "질문은 필수입니다")
        private String question;
        
        private String sessionId;
        private String userId;
        private Map<String, Object> context;
    }

    /**
     * 세션 시작 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StartSession {
        @NotBlank(message = "사용자 ID는 필수입니다")
        private String userId;
        
        private String topic;
        private Map<String, Object> preferences;
    }

    /**
     * 컨텍스트 질문 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContextualQuestion {
        @NotBlank(message = "질문은 필수입니다")
        private String question;
        
        @NotBlank(message = "세션 ID는 필수입니다")
        private String sessionId;
        
        private Map<String, Object> context;
    }

    /**
     * 피드백 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Feedback {
        @NotBlank(message = "세션 ID는 필수입니다")
        private String sessionId;
        
        @NotBlank(message = "피드백은 필수입니다")
        private String feedback;
        
        private Integer rating;
        private String category;
    }
}
