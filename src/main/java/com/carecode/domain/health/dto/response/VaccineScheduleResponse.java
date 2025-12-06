package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 예방접종 스케줄 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccineScheduleResponse {
    private String vaccineName;
    private String description;
    private Integer recommendedAge;
    private String status; // COMPLETED, UPCOMING, OVERDUE
    private String scheduledDate;
    private String completedDate;
    private String notes;
}

