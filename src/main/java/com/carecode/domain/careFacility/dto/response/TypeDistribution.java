package com.carecode.domain.careFacility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 예약 유형별 분포 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeDistribution {
    private String bookingType;
    private String bookingTypeDisplay;
    private Long count;
    private Double percentage;
}

