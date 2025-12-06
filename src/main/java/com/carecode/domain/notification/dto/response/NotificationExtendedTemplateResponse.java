package com.carecode.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 확장된 알림 템플릿 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationExtendedTemplateResponse {
    private NotificationTemplateResponse template;
    private long usageCount;
    private double successRate;
    private LocalDateTime lastUsedAt;
    private List<String> variables;
    private Map<String, Object> defaultValues;
}

