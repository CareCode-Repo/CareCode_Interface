package com.carecode.domain.careFacility.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 예약 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {
    private String childName;
    private Integer childAge;
    private String parentName;
    private String parentPhone;
    private String bookingType; // VISIT, REGULAR, TEMPORARY
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String specialRequirements;
    private String notes;
}

