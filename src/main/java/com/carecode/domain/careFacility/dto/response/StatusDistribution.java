package com.carecode.domain.careFacility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상태별 예약 분포 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusDistribution {
    private String status;
    private String statusDisplay;
    private Long count;
    private Double percentage;
}

