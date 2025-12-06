package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 병원 리뷰 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalReviewResponse {
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

