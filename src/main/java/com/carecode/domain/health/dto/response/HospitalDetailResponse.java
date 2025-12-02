package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 병원 상세 정보 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalDetailResponse {
    private HospitalInfoResponse hospital;
    private List<HospitalReviewResponse> reviews;
    private Double averageRating;
    private Long totalReviews;
    private Long likeCount;
    private Boolean isLiked;
    private List<HospitalInfoResponse> nearbyHospitals;
}

