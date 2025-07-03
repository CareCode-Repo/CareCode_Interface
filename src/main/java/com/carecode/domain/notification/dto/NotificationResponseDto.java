package com.carecode.domain.notification.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 알림 응답 DTO들
 */
public class NotificationResponseDto {
    
    /**
     * 알림 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationResponse {
        private Long id;
        private String notificationType;
        private String title;
        private String message;
        private String priority;
        private Boolean isRead;
        private Boolean isSent;
        private String scheduledAt;
        private String sentAt;
        private String readAt;
        private String createdAt;
    }
    
    /**
     * 알림 설정 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationSettingsResponse {
        private Boolean emailEnabled;
        private Boolean pushEnabled;
        private Boolean smsEnabled;
        private List<String> enabledTypes;
        private String quietHoursStart;
        private String quietHoursEnd;
        private Map<String, Boolean> typeSettings;
    }
    
    /**
     * 알림 통계 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationStatsResponse {
        private Integer totalNotifications;
        private Integer unreadCount;
        private Integer readCount;
        private Map<String, Integer> typeDistribution;
        private Map<String, Integer> priorityDistribution;
        private Map<String, Integer> dailyNotificationCount;
    }
    
    /**
     * 알림 템플릿 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationTemplateResponse {
        private String templateId;
        private String templateName;
        private String notificationType;
        private String title;
        private String message;
        private String description;
        private Boolean isActive;
    }
    
    /**
     * 알림 전송 상태 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationDeliveryStatusResponse {
        private Long notificationId;
        private String deliveryStatus; // PENDING, SENT, FAILED, DELIVERED
        private String deliveryMethod; // EMAIL, PUSH, SMS
        private String sentAt;
        private String deliveredAt;
        private String errorMessage;
    }
} 