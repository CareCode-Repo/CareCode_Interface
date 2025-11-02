package com.carecode.domain.careFacility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 보육시설 관련 응답 DTO 통합 클래스
 */
public class CareFacilityResponse {

    /**
     * 보육시설 정보 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CareFacility {
        private Long id;
        private String name;
        private String facilityType;
        private String address;
        private String phoneNumber;
        private String email;
        private Double latitude;
        private Double longitude;
        private String description;
        private String operatingHours;
        private String website;
        private Double rating;
        private Long reviewCount;
        private Long likeCount;
        private Boolean isLiked;
        private String imageUrl;
        private List<String> amenities;
        private Map<String, Object> additionalInfo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 예약 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Booking {
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

    /**
     * 관리자 예약 상세 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminBookingDetail {
        private Booking booking;
        private CareFacility facility;
        private Map<String, Object> userInfo;
        private List<String> statusHistory;
        private String adminNotes;
    }

    /**
     * 관리자 예약 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminBookingList {
        private List<AdminBookingDetail> bookings;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 관리자 예약 검색 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminBookingSearch {
        private List<AdminBookingDetail> bookings;
        private long totalCount;
        private String searchKeyword;
        private List<String> searchFilters;
        private String sortBy;
        private String sortDirection;
    }

    /**
     * 관리자 예약 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminBookingStats {
        private long totalBookings;
        private long pendingBookings;
        private long confirmedBookings;
        private long cancelledBookings;
        private long completedBookings;
        private Map<String, Long> statusDistribution;
        private Map<String, Long> facilityDistribution;
        private long todayBookings;
        private long thisWeekBookings;
        private long thisMonthBookings;
    }

    /**
     * 예약 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingList {
        private List<Booking> bookings;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 보육시설 검색 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CareFacilitySearch {
        private List<CareFacility> facilities;
        private long totalCount;
        private String searchKeyword;
        private String facilityType;
        private String city;
        private String district;
        private double centerLatitude;
        private double centerLongitude;
        private double radius;
    }

    /**
     * 보육시설 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CareFacilityList {
        private List<CareFacility> facilities;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 보육시설 상세 정보 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CareFacilityDetail {
        private CareFacility facility;
        private List<Booking> recentBookings;
        private List<CareFacility> nearbyFacilities;
        private Map<String, Object> availability;
        private List<String> reviews;
    }

    /**
     * 타입별 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypeStats {
        private String facilityType;
        private long count;
        private double averageRating;
        private long totalBookings;
        private List<String> topFacilities;
    }

    /**
     * 보육시설 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CareFacilityStats {
        private long totalFacilities;
        private long totalBookings;
        private long activeFacilities;
        private Map<String, Long> typeDistribution;
        private List<TypeStats> typeStats;
        private long todayBookings;
        private long thisWeekBookings;
        private long thisMonthBookings;
    }
}
