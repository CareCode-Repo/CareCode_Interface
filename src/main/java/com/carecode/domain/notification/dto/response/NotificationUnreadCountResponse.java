package com.carecode.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 읽지 않은 알림 수 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationUnreadCountResponse {
    private String userId;
    private long unreadCount;
    private Map<String, Long> unreadByType;
    private LocalDateTime lastCheckedAt;
}

