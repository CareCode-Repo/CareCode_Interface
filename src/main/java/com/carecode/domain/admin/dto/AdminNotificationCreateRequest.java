package com.carecode.domain.admin.dto;

import com.carecode.domain.notification.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 어드민 알림 발송 요청.
 * <p>엔티티를 직접 바인딩하지 않고 허용 필드만 받는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminNotificationCreateRequest {

    @NotNull(message = "대상 사용자 ID는 필수입니다")
    private Long userId;

    @NotNull(message = "알림 유형은 필수입니다")
    private Notification.NotificationType notificationType;

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String message;
}
