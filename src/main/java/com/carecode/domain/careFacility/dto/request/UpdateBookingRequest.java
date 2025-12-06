package com.carecode.domain.careFacility.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 예약 수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookingRequest {
    private String childName;
    private Integer childAge;
    private String parentName;
    private String parentPhone;
    private String bookingType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String specialRequirements;
    private String notes;
}

