package com.carecode.domain.admin.dto.response;

import java.util.List;

/**
 * 어드민 대시보드 요약.
 *
 * <p>가입자 추이를 {@code userTrendLabels}·{@code userTrendData} 두 배열로 주는 이유: 차트
 * 라이브러리가 축과 값을 따로 받기 때문이다. 두 배열의 길이와 순서는 같고(최근 6개월, 과거→현재),
 * 값이 없는 달도 0 으로 채워 그래프가 끊기지 않는다.
 */
public record AdminDashboardResponse(
        long userCount,
        long hospitalCount,
        long policyCount,
        List<RecentActivity> recentActivities,
        List<String> userTrendLabels,
        List<Long> userTrendData) {

    /**
     * 최근 활동 한 줄.
     *
     * <p>{@code time} 은 {@code yyyy-MM-dd HH:mm} 으로 미리 다듬은 문자열이다. 종류가 섞인
     * 목록을 한 줄로 정렬하는 데 쓰므로 모양이 같아야 한다.
     */
    public record RecentActivity(String type, String desc, String time) {
    }
}
