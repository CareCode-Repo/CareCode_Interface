package com.carecode.domain.notification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알림 삭제 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeleteRequest {
    @NotNull(message = "알림 ID는 필수입니다")
    private Long notificationId;
}

