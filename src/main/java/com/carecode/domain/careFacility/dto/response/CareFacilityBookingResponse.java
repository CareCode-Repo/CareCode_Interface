package com.carecode.domain.careFacility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 예약 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilityBookingResponse {
    private Long id;
    private Long facilityId;
    private String facilityName;
    private String userId;
    private String userName;
    private String childName;
    private LocalDateTime bookingDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String notes;
    private String contactPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

