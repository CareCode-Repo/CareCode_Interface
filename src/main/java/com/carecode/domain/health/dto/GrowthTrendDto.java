package com.carecode.domain.health.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * 성장 추이 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrowthTrendDto {
    
    private Long childId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Double> heightTrend;
    private List<Double> weightTrend;
} 