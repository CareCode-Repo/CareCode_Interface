package com.carecode.domain.facility.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * "우리 아이가 들어갈 수 있는 곳" — 지역 안의 시설을 입소 확률 순으로.
 *
 * <p>시설 하나를 골라 들어가야 확률을 볼 수 있다면, 어디를 고를지 모르는 사람은 그 숫자를 영영 못 본다.
 * 검색을 뒤집어 답부터 주려고 만든 응답이다.
 *
 * <p>보여주지 않은 것도 함께 센다. 후보 40곳 중 3곳만 나왔을 때 그 사실을 숨기면 "이 동네에 어린이집이
 * 3곳뿐인가" 로 읽힌다. 실제로는 관측이 덜 쌓여 확률을 낼 수 없었을 뿐이다.
 */
@Getter
@Builder
public class AdmissionCandidateResponse {

    private final String region;
    private final int childAgeMonths;

    /** 아이 월령 기준 배정 반. */
    private final String targetClass;

    private final int horizonMonths;

    /** 예측 기준 시점. */
    private final LocalDate targetDate;

    /** 지역에서 후보로 본 시설 수. */
    private final int evaluatedFacilities;

    /** 관측이 모자라 확률을 내지 못한 시설 수. */
    private final int notEnoughDataCount;

    /**
     * 이 기간 예측이 과거에 얼마나 맞았는지. 표본이 부족하면 null.
     * 확률을 나열하면서 그 확률이 믿을 만한지를 말하지 않으면 숫자가 혼자 커진다.
     */
    private final ForecastAccuracyResponse accuracy;

    private final List<Candidate> candidates;

    @Getter
    @Builder
    public static class Candidate {

        private final Long facilityId;
        private final String facilityName;
        private final String address;

        /** 목표 시점까지 자리가 날 확률(0~100). */
        private final Integer probability;

        /** LOW / MEDIUM / HIGH */
        private final String confidence;

        /** 이 확률이 몇 번의 관측에 근거하는지. 같은 확률이면 근거가 많은 쪽이 낫다. */
        private final long observationCount;
        private final long observationDays;

        /** 공공데이터에 적힌 현재 잔여석. 예측과 달리 지금 시점의 값이다. */
        private final Integer availableSpots;

        /** 왜 이 확률이 나왔는지. */
        private final List<String> reasons;
    }
}
