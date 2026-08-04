package com.carecode.domain.health.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 건강 기록 수정 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthUpdateHealthRecordRequest {
    @NotBlank(message = "제목은 필수입니다")
    private String title;

    private String description;

    @NotNull(message = "기록 날짜는 필수입니다")
    private LocalDateTime recordDate;

    private LocalDateTime nextDate;
    private String location;
    private String doctorName;
    private String hospitalName;
    private Boolean isCompleted;

    // ==================== 측정값 ====================

    @DecimalMin(value = "0.0", inclusive = false, message = "키는 0보다 커야 합니다")
    @DecimalMax(value = "250.0", message = "키는 250cm를 넘을 수 없습니다")
    private Double height; // cm

    @DecimalMin(value = "0.0", inclusive = false, message = "몸무게는 0보다 커야 합니다")
    @DecimalMax(value = "200.0", message = "몸무게는 200kg를 넘을 수 없습니다")
    private Double weight; // kg

    @DecimalMin(value = "30.0", message = "체온 값을 확인해주세요")
    @DecimalMax(value = "45.0", message = "체온 값을 확인해주세요")
    private Double temperature; // °C

    private String bloodPressure;

    @Min(value = 0, message = "맥박은 0 이상이어야 합니다")
    @Max(value = 300, message = "맥박 값을 확인해주세요")
    private Integer pulseRate;

    private String vaccineName;
}

