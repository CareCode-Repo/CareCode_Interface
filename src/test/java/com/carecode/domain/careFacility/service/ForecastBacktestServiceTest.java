package com.carecode.domain.careFacility.service;

import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.entity.ForecastAccuracy;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import com.carecode.domain.careFacility.repository.ForecastAccuracyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 백테스트가 정확도를 부풀리지 않는지 확인한다.
 *
 * <p>이 검증에서 가장 위험한 실수는 기준일 이후 관측을 예측 입력에 섞는 것이다(look-ahead).
 * 그러면 "미래를 보고 예측" 하게 되어 어떤 알고리즘이든 훌륭해 보인다.
 */
@DisplayName("예측 정확도 백테스트")
class ForecastBacktestServiceTest {

    private FacilityCapacitySnapshotRepository snapshotRepository;
    private ForecastAccuracyRepository accuracyRepository;
    private ForecastBacktestService service;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(FacilityCapacitySnapshotRepository.class);
        accuracyRepository = mock(ForecastAccuracyRepository.class);
        when(accuracyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new ForecastBacktestService(snapshotRepository, accuracyRepository,
                new AdmissionForecastCalculator(), new ObjectMapper(), 2000);
    }

    @Test
    @DisplayName("관측 기간이 예측 기간보다 짧으면 표본이 없어 아무것도 저장하지 않는다")
    void notEnoughHistoryProducesNothing() {
        givenFacility(1L, weekly(LocalDate.now().minusMonths(2), 10, true));

        assertThat(service.runAll(List.of(6))).isEmpty();
    }

    @Test
    @DisplayName("항상 자리가 있던 시설은 높은 확률을 예측하고 실제도 참이라 정확도가 좋다")
    void alwaysOpenFacilityScoresWell() {
        givenFacility(1L, weekly(LocalDate.now().minusMonths(14), 60, true));

        ForecastAccuracy result = service.runAll(List.of(3)).get(0);

        assertThat(result.getSamples()).isPositive();
        assertThat(result.getActualRate()).isEqualTo(1.0);
        assertThat(result.getBrierScore()).isLessThan(0.1);
        assertThat(result.getHorizonMonths()).isEqualTo(3);
        assertThat(result.getFacilities()).isEqualTo(1);
    }

    @Test
    @DisplayName("기준일 이후 관측은 예측 입력에서 빠진다 — 자리가 뒤늦게 열려도 그 전 예측은 낙관하지 않는다")
    void noLookAhead() {
        // 앞 10개월은 자리 없음, 그 뒤 4개월은 자리 있음.
        List<FacilityCapacitySnapshot> history = new ArrayList<>();
        history.addAll(weekly(LocalDate.now().minusMonths(14), 40, false));
        history.addAll(weekly(LocalDate.now().minusMonths(4), 16, true));
        givenFacility(1L, history);

        ForecastAccuracy result = service.runAll(List.of(1)).get(0);
        List<ForecastBacktestService.Bucket> buckets = buckets(result);

        // 자리가 한 번도 없던 구간에서 계산된 예측은 0~20% 구간에 있어야 한다.
        int lowBucketSamples = buckets.stream().filter(b -> b.from() == 0).mapToInt(ForecastBacktestService.Bucket::samples).sum();
        assertThat(lowBucketSamples)
                .as("자리가 없던 기간의 예측이 낙관적으로 나오면 미래를 본 것이다")
                .isPositive();
        // 그 구간 예측 중 실제로 자리가 난 건은 없거나 극히 적다(마지막 구간 경계 한두 건).
        int lowBucketHits = buckets.stream().filter(b -> b.from() == 0).mapToInt(ForecastBacktestService.Bucket::actualTrue).sum();
        assertThat(lowBucketHits).isLessThan(lowBucketSamples);
    }

    @Test
    @DisplayName("구간표 표본 합계는 전체 표본과 같다")
    void calibrationCoversEverySample() {
        givenFacility(1L, mixed(LocalDate.now().minusMonths(16)));

        ForecastAccuracy result = service.runAll(List.of(3)).get(0);

        assertThat(buckets(result).stream().mapToInt(ForecastBacktestService.Bucket::samples).sum())
                .isEqualTo(result.getSamples());
    }

    @Test
    @DisplayName("기준선(항상 평균으로 답하기)과 함께 기록해 예측이 나은지 판단할 수 있다")
    void recordsBaselineForComparison() {
        givenFacility(1L, mixed(LocalDate.now().minusMonths(16)));

        ForecastAccuracy result = service.runAll(List.of(3)).get(0);

        assertThat(result.getBaselineBrierScore()).isNotNull();
        assertThat(result.betterThanBaseline())
                .isEqualTo(result.getBrierScore() < result.getBaselineBrierScore());
    }

    @Test
    @DisplayName("측정 기간별로 결과를 따로 남긴다")
    void resultPerHorizon() {
        givenFacility(1L, weekly(LocalDate.now().minusMonths(20), 80, true));

        List<ForecastAccuracy> results = service.runAll(List.of(1, 3));

        assertThat(results).hasSize(2);
        assertThat(results.stream().map(ForecastAccuracy::getHorizonMonths)).containsExactly(1, 3);
    }

    @Test
    @DisplayName("대상 시설이 상한을 넘으면 그만큼만 본다 — 주간 작업 시간을 묶는다")
    void capsFacilityCount() {
        when(snapshotRepository.findFacilityIdsWithAtLeast(anyLong())).thenReturn(List.of(1L, 2L, 3L));
        when(snapshotRepository.findHistory(any(), any())).thenReturn(weekly(LocalDate.now().minusMonths(14), 60, true));
        ForecastBacktestService capped = new ForecastBacktestService(snapshotRepository, accuracyRepository,
                new AdmissionForecastCalculator(), new ObjectMapper(), 2);

        ForecastAccuracy result = capped.runAll(List.of(3)).get(0);

        assertThat(result.getFacilities()).isEqualTo(2);
    }

    private List<ForecastBacktestService.Bucket> buckets(ForecastAccuracy accuracy) {
        try {
            return new ObjectMapper().readValue(accuracy.getCalibrationJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<ForecastBacktestService.Bucket>>() {
                    });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void givenFacility(Long facilityId, List<FacilityCapacitySnapshot> history) {
        when(snapshotRepository.findFacilityIdsWithAtLeast(anyLong())).thenReturn(List.of(facilityId));
        when(snapshotRepository.findHistory(eq(facilityId), any())).thenReturn(history);
    }

    /** 주 1회 관측. 공공데이터 시설 동기화 주기와 같다. */
    private static List<FacilityCapacitySnapshot> weekly(LocalDate start, int count, boolean hasSeat) {
        List<FacilityCapacitySnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            snapshots.add(snapshot(start.plusWeeks(i), hasSeat ? 3 : 0));
        }
        return snapshots;
    }

    /** 자리가 있다가 없다가 하는 시설. 확률이 중간 구간에 퍼진다. */
    private static List<FacilityCapacitySnapshot> mixed(LocalDate start) {
        List<FacilityCapacitySnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < 70; i++) {
            snapshots.add(snapshot(start.plusWeeks(i), i % 3 == 0 ? 2 : 0));
        }
        return snapshots;
    }

    private static FacilityCapacitySnapshot snapshot(LocalDate date, int availableSpots) {
        return FacilityCapacitySnapshot.builder()
                .facilityId(1L)
                .observedDate(date)
                .capacity(100)
                .currentEnrollment(100 - availableSpots)
                .availableSpots(availableSpots)
                .build();
    }
}
