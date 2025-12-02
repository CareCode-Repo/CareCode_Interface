package com.carecode.domain.careFacility.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 예약 생성 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilityCreateBookingRequest {
    @NotNull(message = "보육시설 ID는 필수입니다")
    private Long facilityId;
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @NotBlank(message = "아동 이름은 필수입니다")
    private String childName;
    
    @NotNull(message = "예약 날짜는 필수입니다")
    private LocalDateTime bookingDate;
    
    @NotNull(message = "시작 시간은 필수입니다")
    private LocalDateTime startTime;
    
    @NotNull(message = "종료 시간은 필수입니다")
    private LocalDateTime endTime;
    
    private String notes;
    private String contactPhone;
}

