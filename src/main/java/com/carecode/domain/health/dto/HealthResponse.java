package com.carecode.domain.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 건강 관리 관련 응답 DTO 통합 클래스
 */
public class HealthResponse {

    /**
     * 건강 기록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthRecord {
        private Long id;
        private String childId;
        private String recordType;
        private String title;
        private String description;
        private String recordDate;
        private String nextDate;
        private String location;
        private String doctorName;
        private Boolean isCompleted;
        private String createdAt;
        private String updatedAt;
    }

    /**
     * 예방접종 스케줄 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VaccineSchedule {
        private String vaccineName;
        private String description;
        private Integer recommendedAge;
        private String status; // COMPLETED, UPCOMING, OVERDUE
        private String scheduledDate;
        private String completedDate;
        private String notes;
    }

    /**
     * 건강 검진 스케줄 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckupSchedule {
        private String checkupName;
        private String description;
        private Integer recommendedAge;
        private String status; // COMPLETED, UPCOMING, OVERDUE
        private String scheduledDate;
        private String completedDate;
        private String notes;
    }

    /**
     * 건강 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthStats {
        private Integer totalRecords;
        private Integer completedVaccines;
        private Integer pendingVaccines;
        private Integer completedCheckups;
        private Integer pendingCheckups;
        private Map<String, Integer> recordTypeDistribution;
        private List<String> upcomingEvents;
    }

    /**
     * 건강 알림 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthAlert {
        private String alertId;
        private String alertType;
        private String title;
        private String message;
        private String priority; // HIGH, MEDIUM, LOW
        private String dueDate;
        private Boolean isRead;
    }


    /**
     * 병원 리뷰 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HospitalReview {
        private Long id;
        private Long userId;
        private Long hospitalId;
        private String hospitalName;
        private String userName;
        private Integer rating;
        private String content;
        private String createdAt;
        private String updatedAt;
    }

    /**
     * 병원 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HospitalList {
        private List<Hospital> hospitals;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 병원 검색 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HospitalSearch {
        private List<Hospital> hospitals;
        private long totalCount;
        private String searchKeyword;
        private String hospitalType;
        private double centerLatitude;
        private double centerLongitude;
        private double radius;
    }

    /**
     * 근처 병원 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NearbyHospitals {
        private List<Hospital> hospitals;
        private double centerLatitude;
        private double centerLongitude;
        private double radius;
        private int count;
    }

    /**
     * 병원 상세 정보 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HospitalDetail {
        private Hospital hospital;
        private List<HospitalReview> reviews;
        private Double averageRating;
        private Long totalReviews;
        private Long likeCount;
        private Boolean isLiked;
        private List<Hospital> nearbyHospitals;
    }


    /**
     * 성장 추이 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GrowthTrend {
        private String childId;
        private String childName;
        private List<GrowthData> growthData;
        private String period;
        private String trend;
    }

    /**
     * 성장 데이터
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GrowthData {
        private String date;
        private Double height;
        private Double weight;
        private Double headCircumference;
        private String notes;
    }

    /**
     * 건강 기록 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthRecordResponse {
        private Long id;
        private String childId;
        private String childName;
        private String userId;
        private String recordType;
        private String title;
        private String description;
        private String recordDate;
        private String nextDate;
        private String location;
        private String doctorName;
        private String hospitalName;
        private Double height;
        private Double weight;
        private Double temperature;
        private String bloodPressure;
        private Integer pulseRate;
        private String vaccineName;
        private Boolean isCompleted;
        private String createdAt;
        private String updatedAt;
    }

    /**
     * 예방접종 스케줄 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VaccineScheduleResponse {
        private String vaccineName;
        private String description;
        private Integer recommendedAge;
        private String status; // COMPLETED, UPCOMING, OVERDUE
        private String scheduledDate;
        private String completedDate;
        private String notes;
    }

    /**
     * 건강 검진 스케줄 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckupScheduleResponse {
        private String checkupName;
        private String description;
        private Integer recommendedAge;
        private String status; // COMPLETED, UPCOMING, OVERDUE
        private String scheduledDate;
        private String completedDate;
        private String notes;
    }

    /**
     * 건강 통계 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthStatsResponse {
        private Integer totalRecords;
        private Integer completedVaccines;
        private Integer pendingVaccines;
        private Integer completedCheckups;
        private Integer pendingCheckups;
        private Map<String, Integer> recordTypeDistribution;
        private List<String> upcomingEvents;
    }

    /**
     * 건강 알림 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthAlertResponse {
        private String alertId;
        private String alertType;
        private String title;
        private String message;
        private String priority; // HIGH, MEDIUM, LOW
        private String dueDate;
        private Boolean isRead;
    }

    /**
     * 병원 정보 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Hospital {
        private Long id;
        private String name;
        private String type;
        private String address;
        private String phoneNumber;
        private Double latitude;
        private Double longitude;
        private String createdAt;
        private String updatedAt;
    }

    /**
     * 아동 정보 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Child {
        private Long id;
        private Long userId;
        private String name;
        private String birthDate;
        private String gender;
        private String createdAt;
        private String updatedAt;
    }
}