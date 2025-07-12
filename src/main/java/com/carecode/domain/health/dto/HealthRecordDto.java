package com.carecode.domain.health.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 건강 기록 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthRecordDto {
    
    private Long id;
    private Long childId;
    private LocalDate recordDate;
    private Double height;
    private Double weight;
    private Double temperature;
    private String bloodPressure;
    private Integer pulseRate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 