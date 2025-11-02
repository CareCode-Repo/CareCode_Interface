package com.carecode.domain.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 건강 관리 관련 요청 DTO 통합 클래스
 */
public class HealthRequest {

    /**
     * 건강 기록 생성 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateHealthRecord {
        @NotBlank(message = "아동 ID는 필수입니다")
        private String childId;
        
        @NotBlank(message = "기록 타입은 필수입니다")
        private String recordType;
        
        @NotBlank(message = "제목은 필수입니다")
        private String title;
        
        private String description;
        
        @NotNull(message = "기록 날짜는 필수입니다")
        private LocalDateTime recordDate;
        
        private LocalDateTime nextDate;
        private String location;
        private String doctorName;
    }

    /**
     * 건강 기록 수정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateHealthRecord {
        @NotBlank(message = "제목은 필수입니다")
        private String title;
        
        private String description;
        
        @NotNull(message = "기록 날짜는 필수입니다")
        private LocalDateTime recordDate;
        
        private LocalDateTime nextDate;
        private String location;
        private String doctorName;
        private Boolean isCompleted;
    }

    /**
     * 예방접종 스케줄 조회 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VaccineSchedule {
        @NotBlank(message = "아동 ID는 필수입니다")
        private String childId;
        
        private Integer childAge;
        
        @NotNull(message = "생년월일은 필수입니다")
        private LocalDateTime birthDate;
    }

    /**
     * 건강 검진 스케줄 조회 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckupSchedule {
        @NotBlank(message = "아동 ID는 필수입니다")
        private String childId;
        
        private Integer childAge;
        
        @NotNull(message = "생년월일은 필수입니다")
        private LocalDateTime birthDate;
    }

    /**
     * 병원 좋아요 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LikeHospital {
        @NotNull(message = "병원 ID는 필수입니다")
        private Long hospitalId;
    }

    /**
     * 병원 리뷰 작성 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateHospitalReview {
        @NotNull(message = "병원 ID는 필수입니다")
        private Long hospitalId;
        
        @NotNull(message = "평점은 필수입니다")
        private Integer rating;
        
        @NotBlank(message = "리뷰 내용은 필수입니다")
        private String content;
    }

    /**
     * 병원 리뷰 수정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateHospitalReview {
        @NotNull(message = "평점은 필수입니다")
        private Integer rating;
        
        @NotBlank(message = "리뷰 내용은 필수입니다")
        private String content;
    }

    /**
     * 건강 통계 조회 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthStats {
        @NotBlank(message = "사용자 ID는 필수입니다")
        private String userId;
        
        private String childId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    /**
     * 건강 알림 조회 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthAlerts {
        @NotBlank(message = "사용자 ID는 필수입니다")
        private String userId;
        
        private String alertType;
        private Boolean isRead;
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

    /**
     * 아동 정보 생성 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateChild {
        @NotNull(message = "사용자 ID는 필수입니다")
        private Long userId;
        
        @NotBlank(message = "아동 이름은 필수입니다")
        private String name;
        
        @NotNull(message = "생년월일은 필수입니다")
        private LocalDate birthDate;
        
        @NotBlank(message = "성별은 필수입니다")
        private String gender;
    }
}
