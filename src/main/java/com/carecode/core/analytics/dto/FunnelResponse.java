package com.carecode.core.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** 온보딩·핵심가치 퍼널. 각 단계에 도달한 고유 사용자 수를 센다. */
@Getter
@Builder
public class FunnelResponse {

    private LocalDate from;
    private LocalDate to;
    private List<Step> steps;

    @Getter
    @Builder
    public static class Step {
        private String event;
        private String label;
        private long users;
        /** 직전 단계 대비 전환율(%). 첫 단계는 null. */
        private Integer conversionRate;
    }
}
