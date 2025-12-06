package com.carecode.domain.careFacility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 예약 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long facilityId;
    private String facilityName;
    private String userId;
    private String childName;
    private Integer childAge;
    private String parentName;
    private String parentPhone;
    private String bookingType;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String specialRequirements;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

