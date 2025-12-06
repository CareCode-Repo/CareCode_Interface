package com.carecode.domain.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 챗봇 통계 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotStatsDtoResponse {
    private Integer totalSessions;
    private Integer activeSessions;
    private Double averageRating;
    private Map<String, Integer> categoryDistribution;
    private Map<String, Integer> ageGroupDistribution;
}

