package com.carecode.domain.health.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 병원 리뷰 수정 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthUpdateHospitalReviewRequest {
    @NotNull(message = "평점은 필수입니다")
    private Integer rating;
    
    @NotBlank(message = "리뷰 내용은 필수입니다")
    private String content;
}

