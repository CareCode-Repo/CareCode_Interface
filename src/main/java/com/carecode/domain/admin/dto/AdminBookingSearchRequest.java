package com.carecode.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자용 예약 검색 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingSearchRequest {
    private Long facilityId;
    private String userId;
    private String bookingType;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String keyword; // 시설명, 사용자명, 아동명 검색
    private Integer page;
    private Integer size;
}