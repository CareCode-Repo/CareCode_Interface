package com.carecode.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 알림 관련 응답 DTO 통합 클래스
 */
public class NotificationResponse {

    /**
     * 알림 정보 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Notification {
        private Long id;
        private String userId;
        private String notificationType;
        private String title;
        private String message;
        private String priority;
        private boolean isRead;
        private LocalDateTime createdAt;
        private LocalDateTime readAt;
        private LocalDateTime scheduledAt;
        private LocalDateTime sentAt;
        private String deliveryStatus;
    }

    /**
     * 읽지 않은 알림 수 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnreadCount {
        private String userId;
        private long unreadCount;
        private Map<String, Long> unreadByType;
        private LocalDateTime lastCheckedAt;
    }

    /**
     * 확장된 알림 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExtendedStats {
        private Stats basicStats;
        private Map<String, Long> dailyStats;
        private Map<String, Long> weeklyStats;
        private Map<String, Long> monthlyStats;
        private double readRate;
        private double deliveryRate;
        private List<String> topNotificationTypes;
        private List<String> topPriorities;
    }

    /**
     * 확장된 알림 템플릿 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExtendedTemplate {
        private Template template;
        private long usageCount;
        private double successRate;
        private LocalDateTime lastUsedAt;
        private List<String> variables;
        private Map<String, Object> defaultValues;
    }

    /**
     * 알림 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationList {
        private List<Notification> notifications;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 알림 검색 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Search {
        private List<Notification> notifications;
        private long totalCount;
        private String searchKeyword;
        private List<String> searchFilters;
        private String sortBy;
        private String sortDirection;
    }

    /**
     * 알림 요약 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private String userId;
        private long totalNotifications;
        private long unreadNotifications;
        private List<Notification> recentNotifications;
        private List<String> upcomingNotifications;
        private Map<String, Long> typeSummary;
    }

    /**
     * 알림 설정 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Settings {
        private Long id;
        private String userId;
        private String notificationType;
        private Boolean emailEnabled;
        private Boolean pushEnabled;
        private Boolean smsEnabled;
        private Boolean inAppEnabled;
        private String emailAddress;
        private String phoneNumber;
        private String deviceToken;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 알림 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Stats {
        private Integer totalNotifications;
        private Integer unreadCount;
        private Integer readCount;
        private Map<String, Integer> typeDistribution;
        private Map<String, Integer> priorityDistribution;
        private Map<String, Integer> dailyNotificationCount;
    }

    /**
     * 알림 템플릿 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Template {
        private String templateId;
        private String templateName;
        private String notificationType;
        private String title;
        private String message;
        private String description;
        private Boolean isActive;
    }

    /**
     * 알림 전송 상태 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeliveryStatus {
        private Long notificationId;
        private String deliveryStatus; // PENDING, SENT, FAILED, DELIVERED
        private String deliveryMethod; // EMAIL, PUSH, SMS
        private String sentAt;
        private String deliveredAt;
        private String errorMessage;
    }

    /**
     * 알림 상세 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail extends Notification {
        private LocalDateTime deliveredAt;
        private String failureReason;
    }
}
