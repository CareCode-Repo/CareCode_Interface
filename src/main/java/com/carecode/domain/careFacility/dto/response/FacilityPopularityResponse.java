package com.carecode.domain.careFacility.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** 충원율 추이로 본 시설 인기도. 리뷰와 달리 시설이 개입할 수 없는 지표다. */
@Getter
@Builder
public class FacilityPopularityResponse {

    private Long facilityId;
    private String facilityName;

    private boolean available;
    private String unavailableReason;

    private long observationCount;

    /** 평균 충원율(%). 현원/정원. */
    private Integer averageFillRate;

    /** 최근 관측 충원율(%). */
    private Integer latestFillRate;

    /** 관측 중 정원이 꽉 찬 비율(%). 높을수록 대기가 밀린다. */
    private Integer fullRatio;

    /** 충원율 추세. RISING / STABLE / FALLING */
    private String trend;

    /** IN_DEMAND / STEADY / UNDERSUBSCRIBED */
    private String demandLevel;

    /** 충원율이 급락한 시점. 운영 변화 신호일 수 있어 별도로 알린다. */
    private List<LocalDate> sharpDropDates;

    private List<String> reasons;
}
