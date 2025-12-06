package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 근처 병원 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalNearbyResponse {
    private List<HospitalInfoResponse> hospitals;
    private double centerLatitude;
    private double centerLongitude;
    private double radius;
    private int count;
}

