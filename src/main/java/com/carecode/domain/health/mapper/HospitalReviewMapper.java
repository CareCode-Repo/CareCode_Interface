package com.carecode.domain.health.mapper;

import com.carecode.core.util.ResponseMapper;
import com.carecode.domain.health.dto.response.HealthResponse;
import com.carecode.domain.health.dto.response.HospitalReviewResponse;
import com.carecode.domain.health.entity.HospitalReview;
import org.springframework.stereotype.Component;

@Component
public class HospitalReviewMapper implements ResponseMapper<HospitalReview, HospitalReviewResponse> {
    @Override
    public HospitalReviewResponse toResponse(HospitalReview review) {
        return HospitalReviewResponse.builder()
                .id(review.getId())
                .hospitalId(review.getHospital().getId())
                .hospitalName(review.getHospital().getName())
                .userId(review.getUser().getId())
                .userName("사용자" + review.getUser().getId())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().toString() : null)
                .updatedAt(review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null)
                .build();
    }
}


