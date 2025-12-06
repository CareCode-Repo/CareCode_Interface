package com.carecode.domain.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 세션 시작 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotStartSessionRequest {
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    private String topic;
    private Map<String, Object> preferences;
}

