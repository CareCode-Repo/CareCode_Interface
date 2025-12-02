package com.carecode.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 확장된 알림 통계 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationExtendedStatsResponse {
    private NotificationStatsResponse basicStats;
    private Map<String, Long> dailyStats;
    private Map<String, Long> weeklyStats;
    private Map<String, Long> monthlyStats;
    private double readRate;
    private double deliveryRate;
    private List<String> topNotificationTypes;
    private List<String> topPriorities;
}

