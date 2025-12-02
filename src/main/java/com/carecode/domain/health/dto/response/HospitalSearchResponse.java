package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 병원 검색 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalSearchResponse {
    private List<HospitalInfoResponse> hospitals;
    private long totalCount;
    private String searchKeyword;
    private String hospitalType;
    private double centerLatitude;
    private double centerLongitude;
    private double radius;
}

