package com.carecode.domain.notification.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 요청 DTO들
 */
public class NotificationRequestDto {
    
    /**
     * 알림 생성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateNotificationRequest {
        private String userId;
        private String notificationType;
        private String title;
        private String message;
        private String priority;
        private LocalDateTime scheduledAt;
    }
    
    /**
     * 일괄 알림 생성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateBulkNotificationRequest {
        private List<String> userIds;
        private String notificationType;
        private String title;
        private String message;
        private String priority;
        private LocalDateTime scheduledAt;
    }
    
    /**
     * 알림 설정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationSettingsRequest {
        private Boolean emailEnabled;
        private Boolean pushEnabled;
        private Boolean smsEnabled;
        private List<String> enabledTypes;
        private String quietHoursStart;
        private String quietHoursEnd;
    }
    
    /**
     * 알림 읽음 처리 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MarkAsReadRequest {
        private List<Long> notificationIds;
        private Boolean markAllAsRead;
    }
} 