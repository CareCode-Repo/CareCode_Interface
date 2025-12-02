package com.carecode.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 지식 베이스 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotKnowledgeBaseDtoResponse {
    private String entryId;
    private String title;
    private String content;
    private String category;
    private Integer minAge;
    private Integer maxAge;
    private Double relevanceScore;
}

