package com.carecode.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 통계 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long verifiedUsers;
    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUsersThisMonth;
}

