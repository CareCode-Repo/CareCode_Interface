package com.carecode.domain.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 컨텍스트 질문 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotContextualQuestionRequest {
    @NotBlank(message = "질문은 필수입니다")
    private String question;
    
    @NotBlank(message = "세션 ID는 필수입니다")
    private String sessionId;
    
    private Map<String, Object> context;
}

