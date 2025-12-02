package com.carecode.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 지식베이스 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotKnowledgeBaseResponse {
    private String id;
    private String title;
    private String content;
    private String category;
    private List<String> tags;
    private String source;
    private LocalDateTime lastUpdated;
}

