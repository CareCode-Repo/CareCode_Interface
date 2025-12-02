package com.carecode.domain.careFacility.mapper;

import com.carecode.core.util.ResponseMapper;
import com.carecode.domain.careFacility.dto.CareFacilityResponse;
import com.carecode.domain.careFacility.dto.response.CareFacilityInfo;
import com.carecode.domain.careFacility.entity.CareFacility;
import org.springframework.stereotype.Component;

@Component
public class CareFacilityMapper implements ResponseMapper<CareFacility, CareFacilityInfo> {
    @Override
    public CareFacilityInfo toResponse(CareFacility facility) {
        return CareFacilityInfo.builder()
                .id(facility.getId())
                .name(facility.getName())
                .facilityType(facility.getFacilityType() != null ? facility.getFacilityType().name() : null)
                .address(facility.getAddress())
                .phoneNumber(facility.getPhone())
                .email(facility.getEmail())
                .latitude(facility.getLatitude())
                .longitude(facility.getLongitude())
                .description(facility.getDescription())
                .operatingHours(facility.getOperatingHours())
                .website(facility.getWebsite())
                .rating(facility.getRating())
                .reviewCount(facility.getReviewCount() != null ? facility.getReviewCount().longValue() : null)
                .likeCount(null)
                .isLiked(null)
                .imageUrl(null)
                .amenities(null)
                .additionalInfo(null)
                .createdAt(facility.getCreatedAt())
                .updatedAt(facility.getUpdatedAt())
                .build();
    }
}


