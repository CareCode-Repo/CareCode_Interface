package com.carecode.domain.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 챗봇 질문 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotAskQuestionRequest {
    @NotBlank(message = "질문은 필수입니다")
    private String question;
    
    private String sessionId;
    private String userId;
    private Map<String, Object> context;
}

