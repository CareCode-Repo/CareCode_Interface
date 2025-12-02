package com.carecode.domain.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 커뮤니티 통계 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityStatsResponse {
    private Integer totalPosts;
    private Integer totalComments;
    private Integer totalUsers;
    private Map<String, Integer> categoryDistribution;
    private Map<String, Integer> dailyPostCount;
}

