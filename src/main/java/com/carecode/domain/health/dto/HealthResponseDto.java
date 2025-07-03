package com.carecode.domain.health.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 건강 관리 응답 DTO들
 */
public class HealthResponseDto {
    
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
} 