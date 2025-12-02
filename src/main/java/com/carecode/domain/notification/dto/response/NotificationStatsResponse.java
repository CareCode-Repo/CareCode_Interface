package com.carecode.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 알림 통계 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationStatsResponse {
    private Integer totalNotifications;
    private Integer unreadCount;
    private Integer readCount;
    private Map<String, Integer> typeDistribution;
    private Map<String, Integer> priorityDistribution;
    private Map<String, Integer> dailyNotificationCount;
}

