package com.carecode.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일괄 알림 생성 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateBulkRequest {
    @NotNull(message = "사용자 ID 목록은 필수입니다")
    private List<String> userIds;
    
    @NotBlank(message = "알림 타입은 필수입니다")
    private String notificationType;
    
    @NotBlank(message = "제목은 필수입니다")
    private String title;
    
    @NotBlank(message = "메시지는 필수입니다")
    private String message;
    
    private String priority;
    private LocalDateTime scheduledAt;
}

