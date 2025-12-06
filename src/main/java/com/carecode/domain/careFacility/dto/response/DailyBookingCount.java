package com.carecode.domain.careFacility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 일별 예약 수 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyBookingCount {
    private String date;
    private Long count;
}

