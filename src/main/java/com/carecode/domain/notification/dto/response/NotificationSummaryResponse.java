package com.carecode.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 알림 요약 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSummaryResponse {
    private String userId;
    private long totalNotifications;
    private long unreadNotifications;
    private List<NotificationInfoResponse> recentNotifications;
    private List<String> upcomingNotifications;
    private Map<String, Long> typeSummary;
}

