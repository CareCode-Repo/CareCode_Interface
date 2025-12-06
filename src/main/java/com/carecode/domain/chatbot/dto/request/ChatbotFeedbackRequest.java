package com.carecode.domain.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 피드백 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotFeedbackRequest {
    @NotBlank(message = "세션 ID는 필수입니다")
    private String sessionId;
    
    @NotBlank(message = "피드백은 필수입니다")
    private String feedback;
    
    private Integer rating;
    private String category;
}

