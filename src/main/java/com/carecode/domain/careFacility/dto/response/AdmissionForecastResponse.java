package com.carecode.domain.careFacility.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** 입소 가능 시점 예측. 근거 없이 숫자만 주지 않는다. */
@Getter
@Builder
public class AdmissionForecastResponse {

    private Long facilityId;
    private String facilityName;

    /** 예측 산출 가능 여부. false 면 probability 는 null 이다. */
    private boolean available;

    /** 예측을 낼 수 없는 이유. available=true 면 null. */
    private String unavailableReason;

    /** 관측 기간(일). 짧을수록 신뢰도가 낮다. */
    private long observationDays;
    private long observationCount;

    /** 아이 월령 기준 배정 반. */
    private String targetClass;

    /** 목표 시점까지 자리가 날 확률(0~100). */
    private Integer probability;

    /** LOW / MEDIUM / HIGH — 관측량과 변동성으로 정한다. */
    private String confidence;

    /** 예측 기준 시점. */
    private LocalDate targetDate;

    /** 사용자에게 보여줄 근거 문장. */
    private List<String> reasons;

    /**
     * 이 확률이 과거에 얼마나 맞았는지. 표본이 부족하면 null 이다.
     * 확률만 보여주면 사용자는 믿을지 판단할 근거가 없다.
     */
    private ForecastAccuracyResponse accuracy;
}
