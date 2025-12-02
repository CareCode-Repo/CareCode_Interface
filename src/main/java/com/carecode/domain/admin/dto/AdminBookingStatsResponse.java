package com.carecode.domain.admin.dto;

import com.carecode.domain.careFacility.dto.response.StatusDistribution;
import com.carecode.domain.careFacility.dto.response.TypeDistribution;
import com.carecode.domain.careFacility.dto.response.FacilityDistribution;
import com.carecode.domain.careFacility.dto.response.DailyBookingCount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 관리자용 예약 통계 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingStatsResponse {
    private Long totalBookings;
    private Long pendingBookings;
    private Long confirmedBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private Long todayBookings;
    private Long thisWeekBookings;
    private Long thisMonthBookings;
    private Double averageCompletionRate;
    private List<StatusDistribution> statusDistribution;
    private List<TypeDistribution> typeDistribution;
    private List<FacilityDistribution> facilityDistribution;
    private List<DailyBookingCount> dailyBookingCounts;
}