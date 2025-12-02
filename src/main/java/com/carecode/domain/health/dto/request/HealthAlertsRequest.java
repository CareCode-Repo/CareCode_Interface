package com.carecode.domain.health.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 건강 알림 조회 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthAlertsRequest {
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    private String alertType;
    private Boolean isRead;
}

