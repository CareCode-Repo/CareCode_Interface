package com.carecode.domain.careFacility.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 실제 대기 기간 통계. 정원 관측 기반 예측을 실측으로 보정한다. */
@Getter
@Builder
public class WaitlistStatsResponse {

    private Long facilityId;
    private String facilityName;

    /** 통계를 낼 만큼 표본이 모였는지. false 면 아래 값은 null 이다. */
    private boolean available;
    private String unavailableReason;

    /** 입소까지 간 기록 수. 이게 표본 크기다. */
    private int admittedSamples;

    /** 현재 대기 중으로 등록된 사람 수. */
    private long currentlyWaiting;

    /** 입소까지 걸린 기간(일). 평균은 이상치에 흔들려 중앙값을 함께 준다. */
    private Integer averageWaitDays;
    private Integer medianWaitDays;
    private Integer maxWaitDays;

    private List<String> reasons;
}
