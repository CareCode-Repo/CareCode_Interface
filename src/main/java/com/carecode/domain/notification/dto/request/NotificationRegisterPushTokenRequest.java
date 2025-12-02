package com.carecode.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 푸시 토큰 등록 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRegisterPushTokenRequest {
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @NotBlank(message = "푸시 토큰은 필수입니다")
    private String pushToken;
    
    private String deviceType;
    private String appVersion;
}

