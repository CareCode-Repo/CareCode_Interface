package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 건강 알림 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthAlertResponse {
    private String alertId;
    private String alertType;
    private String title;
    private String message;
    private String priority; // HIGH, MEDIUM, LOW
    private String dueDate;
    private Boolean isRead;
}

