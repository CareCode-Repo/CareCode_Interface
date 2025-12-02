package com.carecode.domain.careFacility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 예약 통계 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStats {
    private Long totalBookings;
    private Long confirmedBookings;
    private Long pendingBookings;
    private Long cancelledBookings;
    private Long todayBookings;
}

