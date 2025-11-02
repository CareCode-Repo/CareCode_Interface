package com.carecode.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 관련 요청 DTO 통합 클래스
 */
public class NotificationRequest {

    /**
     * 알림 생성 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Create {
        @NotBlank(message = "사용자 ID는 필수입니다")
        private String userId;
        
        @NotBlank(message = "알림 타입은 필수입니다")
        private String notificationType;
        
        @NotBlank(message = "제목은 필수입니다")
        private String title;
        
        @NotBlank(message = "메시지는 필수입니다")
        private String message;
        
        private String priority;
        private LocalDateTime scheduledAt;
    }

    /**
     * 일괄 알림 생성 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateBulk {
        @NotNull(message = "사용자 ID 목록은 필수입니다")
        private List<String> userIds;
        
        @NotBlank(message = "알림 타입은 필수입니다")
        private String notificationType;
        
        @NotBlank(message = "제목은 필수입니다")
        private String title;
        
        @NotBlank(message = "메시지는 필수입니다")
        private String message;
        
        private String priority;
        private LocalDateTime scheduledAt;
    }

    /**
     * 알림 설정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Settings {
        @NotBlank(message = "사용자 ID는 필수입니다")
        private String userId;
        
        private boolean emailEnabled;
        private boolean pushEnabled;
        private boolean smsEnabled;
        private boolean marketingEnabled;
        private boolean systemEnabled;
        private String quietHoursStart;
        private String quietHoursEnd;
    }

    /**
     * 푸시 토큰 등록 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterPushToken {
        @NotBlank(message = "사용자 ID는 필수입니다")
        private String userId;
        
        @NotBlank(message = "푸시 토큰은 필수입니다")
        private String pushToken;
        
        private String deviceType;
        private String appVersion;
    }

    /**
     * 알림 삭제 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Delete {
        @NotNull(message = "알림 ID는 필수입니다")
        private Long notificationId;
    }

    /**
     * 알림 목록 조회 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationListRequest {
        @NotBlank(message = "사용자 ID는 필수입니다")
        private String userId;
        
        private String notificationType;
        private Boolean isRead;
        private String priority;
        private int page;
        private int size;
        private String sortBy;
        private String sortDirection;
    }

    /**
     * 알림 설정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationSettings {
        private Boolean emailEnabled;
        private Boolean pushEnabled;
        private Boolean smsEnabled;
        private List<String> enabledTypes;
        private String quietHoursStart;
        private String quietHoursEnd;
    }

    /**
     * 알림 읽음 처리 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MarkAsRead {
        private List<Long> notificationIds;
        private Boolean markAllAsRead;
    }

    /**
     * 알림 설정 수정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateSettings {
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
     * 테스트 알림 발송 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendTest {
        private String type; // "PUSH", "EMAIL", "SMS"
        private String title;
        private String message;
    }
}
