package com.carecode.domain.careFacility.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 입소 예측 정확도 측정 결과 한 건(기간별 1회 실행). */
@Entity
@Table(name = "TBL_FORECAST_ACCURACY")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ForecastAccuracy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RUN_DATE", nullable = false)
    private LocalDate runDate;

    @Column(name = "HORIZON_MONTHS", nullable = false)
    private int horizonMonths;

    @Column(name = "SAMPLES", nullable = false)
    private int samples;

    @Column(name = "FACILITIES", nullable = false)
    private int facilities;

    /** 낮을수록 정확하다. 0 이 완벽, 0.25 가 동전 던지기 수준. */
    @Column(name = "BRIER_SCORE", nullable = false)
    private double brierScore;

    /** 항상 평균 발생률로 답했을 때의 점수. 예측이 이보다 낮아야 의미가 있다. */
    @Column(name = "BASELINE_BRIER_SCORE", nullable = false)
    private double baselineBrierScore;

    @Column(name = "ACTUAL_RATE", nullable = false)
    private double actualRate;

    @Column(name = "CALIBRATION_JSON", nullable = false, columnDefinition = "TEXT")
    private String calibrationJson;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    /** 예측이 "평균으로 답하기" 보다 나은가. 아니면 화면에 확률을 자랑할 근거가 없다. */
    public boolean betterThanBaseline() {
        return brierScore < baselineBrierScore;
    }
}
