package com.carecode.domain.careFacility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 시설별 예약 분포 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityDistribution {
    private Long facilityId;
    private String facilityName;
    private Long count;
    private Double percentage;
}

