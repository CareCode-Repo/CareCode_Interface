package com.carecode.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 챗봇 통계 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotStatsResponse {
    private long totalSessions;
    private long totalMessages;
    private long activeUsers;
    private double averageSessionDuration;
    private double satisfactionRate;
    private List<String> popularTopics;
    private Map<String, Long> categoryDistribution;
}

