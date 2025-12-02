package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 성장 추이 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrowthTrendResponse {
    private String childId;
    private String childName;
    private List<GrowthDataResponse> growthData;
    private String period;
    private String trend;
}

