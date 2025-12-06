package com.carecode.domain.health.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 예방접종 스케줄 조회 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthVaccineScheduleRequest {
    @NotBlank(message = "아동 ID는 필수입니다")
    private String childId;
    
    private Integer childAge;
    
    @NotNull(message = "생년월일은 필수입니다")
    private LocalDateTime birthDate;
}

