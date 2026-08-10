package com.carecode.domain.careFacility.service;

import com.carecode.domain.careFacility.dto.response.FacilityPopularityResponse;
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

@DisplayName("충원율 기반 시설 인기도")
class FacilityPopularityServiceTest {

    private FacilityCapacitySnapshotRepository snapshotRepository;
    private FacilityPopularityService service;

    @BeforeEach
    void setUp() {
        CareFacilityRepository facilityRepository = mock(CareFacilityRepository.class);
        snapshotRepository = mock(FacilityCapacitySnapshotRepository.class);
        when(facilityRepository.findById(anyLong()))
                .thenReturn(Optional.of(CareFacility.builder().name("행복어린이집").build()));
        service = new FacilityPopularityService(facilityRepository, snapshotRepository,
                mock(EventLogger.class));
    }

    @Test
    @DisplayName("관측이 부족하면 판단하지 않는다")
    void refusesWithoutEnoughObservations() {
        given(rates(95, 96));

        FacilityPopularityResponse result = service.analyze(1L);

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getUnavailableReason()).contains("최소 4회");
    }

    @Test
    @DisplayName("정원이 없는 관측은 비율을 낼 수 없어 제외한다")
    void skipsSnapshotsWithoutCapacity() {
        List<FacilityCapacitySnapshot> history = new ArrayList<>();
        LocalDate start = LocalDate.now().minusWeeks(6);
        for (int i = 0; i < 6; i++) {
            history.add(FacilityCapacitySnapshot.builder()
                    .facilityId(1L).observedDate(start.plusWeeks(i))
                    .capacity(null).currentEnrollment(50).build());
        }
        when(snapshotRepository.findHistory(anyLong(), any())).thenReturn(history);

        FacilityPopularityResponse result = service.analyze(1L);

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getObservationCount()).isZero();
    }

    @Test
    @DisplayName("정원이 계속 차 있으면 인기 시설로 본다")
    void marksInDemandWhenConsistentlyFull() {
        given(rates(100, 100, 99, 100, 100, 100));

        FacilityPopularityResponse result = service.analyze(1L);

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getDemandLevel()).isEqualTo("IN_DEMAND");
        assertThat(result.getFullRatio()).isEqualTo(100);
        assertThat(result.getReasons()).anyMatch(r -> r.contains("대기가 길 수 있습니다"));
    }

    @Test
    @DisplayName("정원 미달이 지속되면 기피 신호로 본다")
    void marksUndersubscribed() {
        given(rates(55, 52, 50, 48, 51, 49));

        FacilityPopularityResponse result = service.analyze(1L);

        assertThat(result.getDemandLevel()).isEqualTo("UNDERSUBSCRIBED");
        assertThat(result.getReasons()).anyMatch(r -> r.contains("입소가 비교적 쉽습니다"));
    }

    @Test
    @DisplayName("충원율 상승 추세를 잡아낸다")
    void detectsRisingTrend() {
        given(rates(60, 62, 65, 80, 85, 90));

        assertThat(service.analyze(1L).getTrend()).isEqualTo("RISING");
    }

    @Test
    @DisplayName("충원율 하락 추세를 잡아낸다")
    void detectsFallingTrend() {
        given(rates(95, 93, 90, 70, 65, 60));

        assertThat(service.analyze(1L).getTrend()).isEqualTo("FALLING");
    }

    @Test
    @DisplayName("변동이 작으면 안정으로 본다")
    void detectsStableTrend() {
        given(rates(80, 82, 79, 81, 80, 83));

        assertThat(service.analyze(1L).getTrend()).isEqualTo("STABLE");
    }

    @Test
    @DisplayName("급락 시점을 기록한다")
    void recordsSharpDrop() {
        // 4월에 30포인트 급락 (3월 신학기가 아님)
        List<FacilityCapacitySnapshot> history = new ArrayList<>();
        int[] values = {95, 94, 60, 62, 63, 61};
        LocalDate start = LocalDate.of(LocalDate.now().getYear() - 1, 4, 1);
        for (int i = 0; i < values.length; i++) {
            history.add(snapshot(start.plusMonths(i), 100, values[i]));
        }
        when(snapshotRepository.findHistory(anyLong(), any())).thenReturn(history);

        FacilityPopularityResponse result = service.analyze(1L);

        assertThat(result.getSharpDropDates()).hasSize(1);
        assertThat(result.getReasons()).anyMatch(r -> r.contains("크게 떨어진 시점"));
    }

    @Test
    @DisplayName("3월 신학기 전환은 급락으로 보지 않는다")
    void ignoresMarchTransition() {
        List<FacilityCapacitySnapshot> history = new ArrayList<>();
        int[] values = {98, 97, 60, 75, 85, 92};
        // 세 번째 관측이 3월이 되도록 1월부터 시작
        LocalDate start = LocalDate.of(LocalDate.now().getYear() - 1, 1, 1);
        for (int i = 0; i < values.length; i++) {
            history.add(snapshot(start.plusMonths(i), 100, values[i]));
        }
        when(snapshotRepository.findHistory(anyLong(), any())).thenReturn(history);

        assertThat(service.analyze(1L).getSharpDropDates()).isEmpty();
    }

    @Test
    @DisplayName("평균과 최근 충원율을 함께 보여준다")
    void reportsAverageAndLatest() {
        given(rates(80, 80, 80, 90));

        FacilityPopularityResponse result = service.analyze(1L);

        assertThat(result.getAverageFillRate()).isEqualTo(83);
        assertThat(result.getLatestFillRate()).isEqualTo(90);
    }

    private void given(List<FacilityCapacitySnapshot> history) {
        when(snapshotRepository.findHistory(anyLong(), any())).thenReturn(history);
    }

    /** 충원율(%)을 주 단위 관측으로 만든다. 정원 100 기준. */
    private List<FacilityCapacitySnapshot> rates(int... fillRates) {
        List<FacilityCapacitySnapshot> list = new ArrayList<>();
        LocalDate start = LocalDate.now().minusWeeks(fillRates.length);
        for (int i = 0; i < fillRates.length; i++) {
            list.add(snapshot(start.plusWeeks(i), 100, fillRates[i]));
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
