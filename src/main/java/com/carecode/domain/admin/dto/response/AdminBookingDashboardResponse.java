package com.carecode.domain.admin.dto.response;

import com.carecode.domain.admin.dto.AdminBookingListResponse;
import com.carecode.domain.admin.dto.AdminBookingStatsResponse;

import java.util.List;

/**
 * 예약 대시보드 요약.
 *
 * <p>통계와 두 목록을 한 번에 주는 이유: 관리자 화면이 세 번 호출해서 조립하면 그 사이에 들어온
 * 예약 때문에 숫자와 목록이 어긋난다.
 *
 * <p>{@code recentBookings} 는 최근 10건, {@code todayBookings} 는 오늘 들어온 것 중 5건이다.
 * 두 목록은 겹칠 수 있다(오늘 예약은 최근 예약이기도 하다).
 */
public record AdminBookingDashboardResponse(
        AdminBookingStatsResponse stats,
        List<AdminBookingListResponse> recentBookings,
        List<AdminBookingListResponse> todayBookings) {
}
