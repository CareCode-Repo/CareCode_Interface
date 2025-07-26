package com.carecode.domain.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 설정 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceDto {

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