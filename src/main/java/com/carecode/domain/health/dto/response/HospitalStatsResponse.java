package com.carecode.domain.health.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/** 병원 등록 현황. 로그인 없이 볼 수 있는 집계다. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "병원 등록 현황")
public class HospitalStatsResponse {

    @Schema(description = "등록된 병원 수", example = "4292")
    private long total;

    /** 진료과목별 수. type 이 비어 있는 행은 세지 않는다. */
    @Schema(description = "진료과목별 등록 수")
    private Map<String, Long> byType;
}
