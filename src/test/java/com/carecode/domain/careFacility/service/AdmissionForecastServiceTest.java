package com.carecode.domain.careFacility.service;

import com.carecode.domain.careFacility.dto.response.AdmissionForecastResponse;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import com.carecode.core.analytics.EventLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("입소 가능 시점 예측")
class AdmissionForecastServiceTest {

    private CareFacilityRepository facilityRepository;
    private FacilityCapacitySnapshotRepository snapshotRepository;
    private AdmissionForecastService service;

    @BeforeEach
    void setUp() {
        facilityRepository = mock(CareFacilityRepository.class);
        snapshotRepository = mock(FacilityCapacitySnapshotRepository.class);
        when(facilityRepository.findById(anyLong()))
                .thenReturn(Optional.of(CareFacility.builder().name("행복어린이집").build()));
        service = new AdmissionForecastService(facilityRepository, snapshotRepository,
                mock(EventLogger.class));
    }

    @Test
    @DisplayName("관측이 없으면 확률을 만들어내지 않는다")
    void refusesWithoutObservations() {
        givenSnapshots(List.of());

        AdmissionForecastResponse result = service.forecast(1L, 12, 6);

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getProbability()).isNull();
        assertThat(result.getUnavailableReason()).contains("관측 이력이 아직 없습니다");
    }

    @Test
    @DisplayName("관측 횟수가 모자라면 추세로 보지 않는다")
    void refusesWithTooFewObservations() {
        givenSnapshots(weekly(3, 100, 100));

        AdmissionForecastResponse result = service.forecast(1L, 12, 6);

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getUnavailableReason()).contains("최소 4회 필요");
    }

    @Test
    @DisplayName("관측 기간이 짧으면 판단을 보류한다")
    void refusesWithShortObservationWindow() {
        // 6회지만 하루 간격이라 기간이 짧다
        List<FacilityCapacitySnapshot> daily = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            daily.add(snapshot(LocalDate.now().minusDays(6 - i), 100, 100));
        }
        givenSnapshots(daily);

        AdmissionForecastResponse result = service.forecast(1L, 12, 6);

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getUnavailableReason()).contains("관측 기간이");
    }

    @Test
    @DisplayName("계속 만원이었으면 확률이 낮게 나온다")
    void lowProbabilityWhenAlwaysFull() {
        givenSnapshots(weekly(20, 100, 100));

        // 신학기가 끼지 않도록 짧은 기간으로 본다
        AdmissionForecastResponse result = service.forecast(1L, 12, 1);

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getProbability()).isLessThan(30);
        assertThat(result.getReasons()).anyMatch(r -> r.contains("자리가 열린 적이 없습니다"));
    }

    @Test
    @DisplayName("자리가 자주 열렸으면 확률이 높게 나온다")
    void highProbabilityWhenSeatsOpenOften() {
        List<FacilityCapacitySnapshot> history = new ArrayList<>();
        LocalDate start = LocalDate.now().minusWeeks(20);
        for (int i = 0; i < 20; i++) {
            // 만원과 여석을 반복 — 자리가 계속 열리는 시설
            int enrolled = i % 2 == 0 ? 100 : 95;
            history.add(snapshot(start.plusWeeks(i), 100, enrolled));
        }
        givenSnapshots(history);

        AdmissionForecastResponse result = service.forecast(1L, 12, 1);

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getProbability()).isGreaterThan(60);
    }

    @Test
    @DisplayName("확률이 100%가 되지는 않는다")
    void neverReturnsCertainty() {
        List<FacilityCapacitySnapshot> history = new ArrayList<>();
        LocalDate start = LocalDate.now().minusWeeks(30);
        for (int i = 0; i < 30; i++) {
            history.add(snapshot(start.plusWeeks(i), 100, i % 2 == 0 ? 100 : 50));
        }
        givenSnapshots(history);

        AdmissionForecastResponse result = service.forecast(1L, 12, 12);

        assertThat(result.getProbability()).isLessThanOrEqualTo(95);
    }

    @Test
    @DisplayName("예측 기간에 3월이 포함되면 근거에 신학기를 남긴다")
    void mentionsNewTermWhenSpanned() {
        givenSnapshots(weekly(20, 100, 100));

        // 12개월을 보면 반드시 3월이 포함된다
        AdmissionForecastResponse result = service.forecast(1L, 12, 12);

        assertThat(result.getReasons()).anyMatch(r -> r.contains("신학기"));
        assertThat(result.getProbability()).isGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("아이 월령으로 배정 반을 계산한다")
    void resolvesTargetClass() {
        givenSnapshots(weekly(20, 100, 100));

        assertThat(service.forecast(1L, 6, 6).getTargetClass()).isEqualTo("0세반");
        assertThat(service.forecast(1L, 18, 6).getTargetClass()).isEqualTo("1세반");
        assertThat(service.forecast(1L, 70, 6).getTargetClass()).isEqualTo("5세반 이상");
    }

    @Test
    @DisplayName("관측이 적으면 신뢰도를 낮게 표기한다")
    void reportsLowConfidenceOnThinData() {
        givenSnapshots(weekly(10, 100, 100));

        assertThat(service.forecast(1L, 12, 1).getConfidence()).isEqualTo("LOW");
    }

    private void givenSnapshots(List<FacilityCapacitySnapshot> snapshots) {
        when(snapshotRepository.findHistory(anyLong(), any())).thenReturn(snapshots);
    }

    /** 주 1회 관측을 count 회 만든다. */
    private List<FacilityCapacitySnapshot> weekly(int count, int capacity, int enrolled) {
        List<FacilityCapacitySnapshot> list = new ArrayList<>();
        LocalDate start = LocalDate.now().minusWeeks(count);
        for (int i = 0; i < count; i++) {
            list.add(snapshot(start.plusWeeks(i), capacity, enrolled));
        }
        return list;
    }

    private FacilityCapacitySnapshot snapshot(LocalDate date, int capacity, int enrolled) {
        return FacilityCapacitySnapshot.builder()
                .facilityId(1L)
                .observedDate(date)
                .capacity(capacity)
                .currentEnrollment(enrolled)
                .availableSpots(Math.max(0, capacity - enrolled))
                .build();
    }
}
