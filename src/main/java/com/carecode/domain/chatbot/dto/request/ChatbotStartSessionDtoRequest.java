package com.carecode.domain.chatbot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 세션 시작 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotStartSessionDtoRequest {
    private String userId;
    private Integer childAge;
    private String childGender;
    private String parentType;
    private String concerns;
}

