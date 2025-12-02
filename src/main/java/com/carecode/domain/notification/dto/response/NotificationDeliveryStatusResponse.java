package com.carecode.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알림 전송 상태 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeliveryStatusResponse {
    private Long notificationId;
    private String deliveryStatus; // PENDING, SENT, FAILED, DELIVERED
    private String deliveryMethod; // EMAIL, PUSH, SMS
    private String sentAt;
    private String deliveredAt;
    private String errorMessage;
}

