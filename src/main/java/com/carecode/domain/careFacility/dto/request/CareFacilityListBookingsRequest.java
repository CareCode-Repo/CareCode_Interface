package com.carecode.domain.careFacility.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 예약 목록 조회 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilityListBookingsRequest {
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int page;
    private int size;
}

