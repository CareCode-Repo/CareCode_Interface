package com.carecode.domain.health.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 건강 통계 조회 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatsRequest {
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    private String childId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}

