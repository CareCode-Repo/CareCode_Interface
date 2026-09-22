package com.carecode.domain.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 알림 정보 응답 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationInfoResponse {
    private Long id;
    private String userId;
    private String notificationType;
    private String title;
    private String message;
    private String priority;
    // 없으면 JSON 키가 "read" 가 되어 프런트가 모든 알림을 안 읽음으로 표시했다.
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private String deliveryStatus;
}

