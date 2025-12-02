package com.carecode.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자 활동 통계 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityResponse {
    private String userId;
    private String userName;
    private LocalDateTime lastLoginAt;
    private int loginCount;
    private int postCount;
    private int commentCount;
    private int bookingCount;
    private LocalDateTime createdAt;
}

