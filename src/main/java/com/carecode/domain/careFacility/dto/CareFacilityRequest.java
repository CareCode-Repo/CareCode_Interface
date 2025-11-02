package com.carecode.domain.careFacility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 보육시설 관련 요청 DTO 통합 클래스
 */
public class CareFacilityRequest {

    /**
     * 보육시설 검색 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Search {
        private String keyword;
        private String facilityType;
        private String city;
        private String district;
        private Double latitude;
        private Double longitude;
        private Double radius;
        private String sortBy;
        private String sortDirection;
        private int page;
        private int size;
    }

    /**
     * 예약 생성 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateBooking {
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

    /**
     * 예약 수정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateBooking {
        @NotNull(message = "예약 날짜는 필수입니다")
        private LocalDateTime bookingDate;
        
        @NotNull(message = "시작 시간은 필수입니다")
        private LocalDateTime startTime;
        
        @NotNull(message = "종료 시간은 필수입니다")
        private LocalDateTime endTime;
        
        private String notes;
        private String contactPhone;
    }

    /**
     * 관리자 예약 검색 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminBookingSearch {
        private Long facilityId;
        private String userId;
        private String childName;
        private String status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String sortBy;
        private String sortDirection;
        private int page;
        private int size;
    }

    /**
     * 관리자 상태 업데이트 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminStatusUpdate {
        @NotNull(message = "예약 ID는 필수입니다")
        private Long bookingId;
        
        @NotBlank(message = "상태는 필수입니다")
        private String status;
        
        private String adminNotes;
    }

    /**
     * 예약 목록 조회 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListBookings {
        @NotBlank(message = "사용자 ID는 필수입니다")
        private String userId;
        
        private String status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private int page;
        private int size;
    }
}
