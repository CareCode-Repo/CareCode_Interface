package com.carecode.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 알림 상세 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDetailResponse extends NotificationInfoResponse {
    private LocalDateTime deliveredAt;
    private String failureReason;
}

