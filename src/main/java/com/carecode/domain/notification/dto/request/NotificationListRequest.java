package com.carecode.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알림 목록 조회 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListRequest {
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    private String notificationType;
    private Boolean isRead;
    private String priority;
    private int page;
    private int size;
    private String sortBy;
    private String sortDirection;
}

