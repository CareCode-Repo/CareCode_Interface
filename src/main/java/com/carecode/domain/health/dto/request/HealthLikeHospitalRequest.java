package com.carecode.domain.health.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 병원 좋아요 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthLikeHospitalRequest {
    @NotNull(message = "병원 ID는 필수입니다")
    private Long hospitalId;
}

