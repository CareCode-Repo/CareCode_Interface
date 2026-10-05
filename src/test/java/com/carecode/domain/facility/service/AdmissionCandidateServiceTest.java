package com.carecode.domain.facility.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.exception.BusinessException;
import com.carecode.domain.facility.dto.response.AdmissionCandidateResponse;
import com.carecode.domain.facility.entity.CareFacility;
import com.carecode.domain.facility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.facility.repository.CareFacilityRepository;
import com.carecode.domain.facility.repository.FacilityCapacitySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 아이 기준 입소 후보.
 *
 * <p>시설을 먼저 고르지 않아도 답을 주는 것이 이 기능의 전부다. 그래서 검증할 것도 둘뿐이다 —
 * **순서가 맞는가**, 그리고 **모르는 것을 지어내지 않는가**.
 */
@DisplayName("아이 기준 입소 후보 추천")
class AdmissionCandidateServiceTest {

    private CareFacilityRepository facilityRepository;
    private FacilityCapacitySnapshotRepository snapshotRepository;
    private AdmissionCandidateService service;

    @BeforeEach
    void setUp() {
        facilityRepository = mock(CareFacilityRepository.class);
        snapshotRepository = mock(FacilityCapacitySnapshotRepository.class);

        ForecastAccuracyService accuracyService = mock(ForecastAccuracyService.class);
        when(accuracyService.describeFor(anyInt(), any())).thenReturn(Optional.empty());

        service = new AdmissionCandidateService(facilityRepository, snapshotRepository,
                new AdmissionForecastCalculator(), accuracyService, mock(EventLogger.class));
    }

    @Test
    @DisplayName("자리가 자주 났던 곳을 위에 둔다")
    void ranksByProbability() {
        CareFacility rarely = facility(1L, "꽉찬어린이집");
        CareFacility often = facility(2L, "여유어린이집");
        givenCandidates(rarely, often);

        List<FacilityCapacitySnapshot> history = new ArrayList<>();
        // 꽉찬어린이집: 12주 내내 정원이 가득 찼다.
        history.addAll(weekly(1L, 12, 100, 100));
        // 여유어린이집: 같은 기간 내내 자리가 있었다.
        history.addAll(weekly(2L, 12, 100, 80));
        givenHistories(history);

        AdmissionCandidateResponse result = service.recommend("성동구", 18, 6, 10);

        assertThat(result.getCandidates()).hasSize(2);
        assertThat(result.getCandidates().get(0).getFacilityName()).isEqualTo("여유어린이집");
        assertThat(result.getCandidates().get(0).getProbability())
                .isGreaterThan(result.getCandidates().get(1).getProbability());
    }

    @Test
    @DisplayName("관측이 모자란 곳은 확률을 지어내지 않고 센다")
    void countsFacilitiesWithoutEnoughData() {
        givenCandidates(facility(1L, "관측많은곳"), facility(2L, "새로생긴곳"));
        List<FacilityCapacitySnapshot> history = new ArrayList<>(weekly(1L, 12, 100, 80));
        // 새로생긴곳은 관측이 2회뿐이라 추세라고 부를 수 없다(MIN_OBSERVATIONS=4).
        history.addAll(weekly(2L, 2, 100, 90));
        givenHistories(history);

        AdmissionCandidateResponse result = service.recommend("성동구", 18, 6, 10);

        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getCandidates().get(0).getFacilityName()).isEqualTo("관측많은곳");
        // 후보 2곳 중 1곳만 나왔다는 사실을 숨기면 "이 동네에 한 곳뿐인가" 로 읽힌다.
        assertThat(result.getEvaluatedFacilities()).isEqualTo(2);
        assertThat(result.getNotEnoughDataCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("요청한 개수까지만 돌려준다")
    void respectsLimit() {
        List<CareFacility> many = new ArrayList<>();
        List<FacilityCapacitySnapshot> history = new ArrayList<>();
        for (long id = 1; id <= 5; id++) {
            many.add(facility(id, "어린이집" + id));
            history.addAll(weekly(id, 12, 100, 80));
        }
        when(facilityRepository.findAdmissionCandidates(anyString(), anyInt(), any())).thenReturn(many);
        givenHistories(history);

        AdmissionCandidateResponse result = service.recommend("성동구", 18, 6, 2);

        assertThat(result.getCandidates()).hasSize(2);
        assertThat(result.getEvaluatedFacilities()).isEqualTo(5);
    }

    @Test
    @DisplayName("시설마다 따로 묻지 않고 관측을 한 번에 읽는다")
    void loadsHistoriesInOneQuery() {
        givenCandidates(facility(1L, "가"), facility(2L, "나"), facility(3L, "다"));
        givenHistories(List.of());

        service.recommend("성동구", 18, 6, 10);

        // 후보가 늘 때마다 질의가 늘면 한 구를 훑는 것만으로 수백 번 왕복한다.
        org.mockito.Mockito.verify(snapshotRepository, org.mockito.Mockito.times(1))
                .findHistories(any(), any());
        org.mockito.Mockito.verify(snapshotRepository, org.mockito.Mockito.never())
                .findHistory(any(), any());
    }

    @Test
    @DisplayName("아이 월령 기준 반 이름을 시설 상세와 같은 규칙으로 붙인다")
    void usesSameClassNamingAsDetail() {
        givenCandidates(facility(1L, "가"));
        givenHistories(List.of());

        AdmissionCandidateResponse result = service.recommend("성동구", 30, 6, 10);

        assertThat(result.getTargetClass())
                .isEqualTo(AdmissionForecastCalculator.resolveClassName(30))
                .isEqualTo("2세반");
    }

    @Test
    @DisplayName("지역이나 월령이 없으면 계산하지 않는다")
    void requiresRegionAndAge() {
        assertThatThrownBy(() -> service.recommend("  ", 18, 6, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지역");

        assertThatThrownBy(() -> service.recommend("성동구", null, 6, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("월령");
    }

    @Test
    @DisplayName("지역에 후보가 없으면 빈 목록을 준다")
    void emptyRegion() {
        when(facilityRepository.findAdmissionCandidates(anyString(), anyInt(), any()))
                .thenReturn(List.of());

        AdmissionCandidateResponse result = service.recommend("없는구", 18, 6, 10);

        assertThat(result.getCandidates()).isEmpty();
        assertThat(result.getEvaluatedFacilities()).isZero();
    }

    private void givenCandidates(CareFacility... facilities) {
        when(facilityRepository.findAdmissionCandidates(anyString(), anyInt(), any()))
                .thenReturn(List.of(facilities));
    }

    private void givenHistories(List<FacilityCapacitySnapshot> snapshots) {
        when(snapshotRepository.findHistories(any(), any(LocalDate.class)))
                .thenReturn(snapshots);
    }

    private CareFacility facility(Long id, String name) {
        return CareFacility.builder().id(id).name(name).address("서울 성동구").build();
    }

    private List<FacilityCapacitySnapshot> weekly(long facilityId, int count, int capacity, int enrolled) {
        List<FacilityCapacitySnapshot> list = new ArrayList<>();
        LocalDate start = LocalDate.now().minusWeeks(count);
        for (int i = 0; i < count; i++) {
            list.add(FacilityCapacitySnapshot.builder()
                    .facilityId(facilityId)
                    .observedDate(start.plusWeeks(i))
                    .capacity(capacity)
                    .currentEnrollment(enrolled)
                    .availableSpots(Math.max(0, capacity - enrolled))
                    .build());
        }
        return list;
    }
}
