package com.carecode.domain.health.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 건강 관리 요청 DTO들
 */
public class HealthRequestDto {
    
    /**
     * 건강 기록 생성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateHealthRecordRequest {
        private String childId;
        private String recordType;
        private String title;
        private String description;
        private LocalDateTime recordDate;
        private LocalDateTime nextDate;
        private String location;
        private String doctorName;
    }
    
    /**
     * 건강 기록 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateHealthRecordRequest {
        private String title;
        private String description;
        private LocalDateTime recordDate;
        private LocalDateTime nextDate;
        private String location;
        private String doctorName;
        private Boolean isCompleted;
    }
    
    /**
     * 예방접종 스케줄 조회 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VaccineScheduleRequest {
        private String childId;
        private Integer childAge;
        private LocalDateTime birthDate;
    }
    
    /**
     * 건강 검진 스케줄 조회 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckupScheduleRequest {
        private String childId;
        private Integer childAge;
        private LocalDateTime birthDate;
    }
} 