package com.carecode.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 알림 설정 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsResponse {
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

