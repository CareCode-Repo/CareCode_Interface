package com.carecode.domain.careFacility.dto;

import com.carecode.domain.careFacility.entity.CareFacilityBooking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 육아 시설 예약 관련 DTO
 */
public class CareFacilityBookingDto {

    /**
     * 예약 생성 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBookingRequest {
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

    /**
     * 예약 수정 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateBookingRequest {
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

    /**
     * 예약 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingResponse {
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

    /**
     * 관리자용 예약 상세 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminBookingDetailResponse {
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

    /**
     * 관리자용 예약 목록 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminBookingListResponse {
        private Long id;
        private Long facilityId;
        private String facilityName;
        private String userId;
        private String userName;
        private String childName;
        private String bookingType;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime createdAt;
    }

    /**
     * 관리자용 예약 검색 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminBookingSearchRequest {
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

    /**
     * 관리자용 예약 검색 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminBookingSearchResponse {
        private List<AdminBookingListResponse> bookings;
        private Long totalElements;
        private Integer totalPages;
        private Integer currentPage;
        private Integer pageSize;
        private Boolean hasNext;
        private Boolean hasPrevious;
    }

    /**
     * 관리자용 예약 상태 변경 요청 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminStatusUpdateRequest {
        private String status;
        private String reason; // 취소 사유 등
        private String adminNote; // 관리자 메모
    }

    /**
     * 관리자용 예약 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminBookingStatsResponse {
        private Long totalBookings;
        private Long pendingBookings;
        private Long confirmedBookings;
        private Long completedBookings;
        private Long cancelledBookings;
        private Long todayBookings;
        private Long thisWeekBookings;
        private Long thisMonthBookings;
        private Double averageCompletionRate;
        private List<StatusDistribution> statusDistribution;
        private List<TypeDistribution> typeDistribution;
        private List<FacilityDistribution> facilityDistribution;
        private List<DailyBookingCount> dailyBookingCounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDistribution {
        private String status;
        private String statusDisplay;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeDistribution {
        private String bookingType;
        private String bookingTypeDisplay;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacilityDistribution {
        private Long facilityId;
        private String facilityName;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyBookingCount {
        private String date;
        private Long count;
    }

    /**
     * 예약 목록 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingListResponse {
        private Long id;
        private Long facilityId;
        private String facilityName;
        private String childName;
        private String bookingType;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime createdAt;
    }

    /**
     * 예약 통계 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingStats {
        private Long totalBookings;
        private Long confirmedBookings;
        private Long pendingBookings;
        private Long cancelledBookings;
        private Long todayBookings;
    }

    /**
     * Entity를 DTO로 변환
     */
    public static BookingResponse fromEntity(CareFacilityBooking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .facilityId(booking.getFacility().getId())
                .facilityName(booking.getFacility().getName())
                .userId(booking.getUserId())
                .childName(booking.getChildName())
                .childAge(booking.getChildAge())
                .parentName(booking.getParentName())
                .parentPhone(booking.getParentPhone())
                .bookingType(booking.getBookingType().name())
                .status(booking.getStatus().name())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .specialRequirements(booking.getSpecialRequirements())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    /**
     * Entity를 목록 DTO로 변환
     */
    public static BookingListResponse toListResponse(CareFacilityBooking booking) {
        return BookingListResponse.builder()
                .id(booking.getId())
                .facilityId(booking.getFacility().getId())
                .facilityName(booking.getFacility().getName())
                .childName(booking.getChildName())
                .bookingType(booking.getBookingType().name())
                .status(booking.getStatus().name())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .createdAt(booking.getCreatedAt())
                .build();
    }
} 