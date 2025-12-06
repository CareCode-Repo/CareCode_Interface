package com.carecode.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알림 설정 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsRequest {
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean smsEnabled;
    private boolean marketingEnabled;
    private boolean systemEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;
}

