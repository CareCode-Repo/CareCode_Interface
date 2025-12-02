package com.carecode.domain.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 테스트 알림 발송 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSendTestRequest {
    private String type; // "PUSH", "EMAIL", "SMS"
    private String title;
    private String message;
}

