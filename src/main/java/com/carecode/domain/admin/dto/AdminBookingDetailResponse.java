package com.carecode.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자용 예약 상세 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingDetailResponse {
    private Long id;
    private Long facilityId;
    private String facilityName;
    private String facilityAddress;
    private String facilityPhone;
    private String userId;
    private String userName;
    private String userEmail;
    private String childName;
    private Integer childAge;
    private String parentName;
    private String parentPhone;
    private String bookingType;
    private String bookingTypeDisplay;
    private String status;
    private String statusDisplay;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private String specialRequirements;
    private String notes;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

