package com.carecode.domain.health.mapper;

import com.carecode.core.util.ResponseMapper;
import com.carecode.domain.health.dto.response.HealthResponse;
import com.carecode.domain.health.dto.response.HospitalInfoResponse;
import com.carecode.domain.health.entity.Hospital;
import org.springframework.stereotype.Component;

@Component
public class HospitalMapper implements ResponseMapper<Hospital, HospitalInfoResponse> {
    @Override
    public HospitalInfoResponse toResponse(Hospital hospital) {
        return HospitalInfoResponse.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .type(hospital.getType())
                .address(hospital.getAddress())
                .phoneNumber(hospital.getPhone())
                .latitude(hospital.getLatitude())
                .longitude(hospital.getLongitude())
                .createdAt(hospital.getCreatedAt() != null ? hospital.getCreatedAt().toString() : null)
                .updatedAt(hospital.getUpdatedAt() != null ? hospital.getUpdatedAt().toString() : null)
                .build();
    }
}


