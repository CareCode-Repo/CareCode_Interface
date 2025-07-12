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

    /**
     * 푸시 알림 토큰 등록 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterPushTokenRequest {
        private String token;
        private String deviceType; // "ANDROID", "IOS", "WEB"
        private String deviceId;
    }

    /**
     * 알림 설정 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateNotificationSettingsRequest {
        private boolean policyNotification;
        private boolean facilityNotification;
        private boolean communityNotification;
        private boolean chatbotNotification;
        private boolean emailNotification;
        private boolean pushNotification;
        private boolean smsNotification;
        private String quietHoursStart;
        private String quietHoursEnd;
    }

    /**
     * 테스트 알림 발송 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendTestNotificationRequest {
        private String type; // "PUSH", "EMAIL", "SMS"
        private String title;
        private String message;
    }
} 