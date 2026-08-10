package com.carecode.core.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** 가입일 기준 코호트 리텐션. */
@Getter
@Builder
public class RetentionResponse {

    private List<Cohort> cohorts;

    @Getter
    @Builder
    public static class Cohort {
        private LocalDate signUpDate;
        private long signedUp;
        /** D1/D7/D30 잔존율(%). 아직 그날이 오지 않았으면 null. */
        private Integer day1;
        private Integer day7;
        private Integer day30;
    }
}
