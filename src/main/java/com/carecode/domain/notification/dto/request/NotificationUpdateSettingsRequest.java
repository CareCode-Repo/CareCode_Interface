package com.carecode.domain.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알림 설정 수정 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationUpdateSettingsRequest {
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

