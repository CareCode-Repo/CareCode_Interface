package com.carecode.domain.careFacility.service;

import com.carecode.domain.careFacility.dto.response.ForecastAccuracyResponse;
import com.carecode.domain.careFacility.entity.ForecastAccuracy;
import com.carecode.domain.careFacility.repository.ForecastAccuracyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 표본이 적은 정확도는 보여주지 않는다. "표본 3건 중 3건 적중 = 100%" 같은 숫자가
 * 근거 없는 확률보다 더 나쁘게 오해를 만든다.
 */
@DisplayName("예측 정확도 노출 규칙")
class ForecastAccuracyServiceTest {

    private ForecastAccuracyRepository repository;
    private ForecastAccuracyService service;

    @BeforeEach
    void setUp() {
        repository = mock(ForecastAccuracyRepository.class);
        service = new ForecastAccuracyService(repository, new ObjectMapper());
    }

    @Test
    @DisplayName("측정 기록이 없으면 비운다")
    void noMeasurement() {
        when(repository.findFirstByHorizonMonthsOrderByRunDateDesc(anyInt())).thenReturn(Optional.empty());

        assertThat(service.describeFor(6, 70)).isEmpty();
    }

    @Test
    @DisplayName("전체 표본이 30건 미만이면 내보내지 않는다")
    void tooFewSamples() {
        given(accuracy(29, "[{\"from\":60,\"to\":80,\"samples\":29,\"actualTrue\":20}]"));

        assertThat(service.describeFor(6, 70)).isEmpty();
    }

    @Test
    @DisplayName("확률이 속한 구간의 실제 적중률을 붙여 준다")
    void matchesBucket() {
        given(accuracy(120, "[{\"from\":40,\"to\":60,\"samples\":50,\"actualTrue\":20},"
                + "{\"from\":60,\"to\":80,\"samples\":70,\"actualTrue\":49}]"));

        ForecastAccuracyResponse response = service.describeFor(6, 72).orElseThrow();

        assertThat(response.getMatchedBucket()).isNotNull();
        assertThat(response.getMatchedBucket().getFrom()).isEqualTo(60);
        assertThat(response.getMatchedBucket().getActualRate()).isEqualTo(0.7);
        assertThat(response.getSamples()).isEqualTo(120);
        // 상세 응답에는 구간표 전체를 싣지 않는다.
        assertThat(response.getCalibration()).isNull();
    }

    @Test
    @DisplayName("구간 표본이 10건 미만이면 그 구간은 숨긴다")
    void bucketTooSmall() {
        given(accuracy(40, "[{\"from\":60,\"to\":80,\"samples\":5,\"actualTrue\":5},"
                + "{\"from\":0,\"to\":20,\"samples\":35,\"actualTrue\":2}]"));

        ForecastAccuracyResponse response = service.describeFor(6, 70).orElseThrow();

        assertThat(response.getMatchedBucket()).isNull();
        assertThat(response.getSamples()).isEqualTo(40);
    }

    @Test
    @DisplayName("공개용 조회는 구간표 전체를 준다")
    void publicViewIncludesCalibration() {
        given(accuracy(80, "[{\"from\":0,\"to\":20,\"samples\":40,\"actualTrue\":4},"
                + "{\"from\":80,\"to\":100,\"samples\":40,\"actualTrue\":36}]"));

        List<ForecastAccuracyResponse> results = service.latestByHorizon(List.of(6));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCalibration()).hasSize(2);
        assertThat(results.get(0).getCalibration().get(1).getActualRate()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("구간표를 읽지 못해도 전체 지표는 살린다")
    void brokenCalibrationJson() {
        given(accuracy(80, "not-json"));

        ForecastAccuracyResponse response = service.describeFor(6, 70).orElseThrow();

        assertThat(response.getSamples()).isEqualTo(80);
        assertThat(response.getMatchedBucket()).isNull();
    }

    private void given(ForecastAccuracy accuracy) {
        when(repository.findFirstByHorizonMonthsOrderByRunDateDesc(anyInt())).thenReturn(Optional.of(accuracy));
    }

    private static ForecastAccuracy accuracy(int samples, String calibrationJson) {
        return ForecastAccuracy.builder()
                .runDate(LocalDate.now())
                .horizonMonths(6)
                .samples(samples)
                .facilities(12)
                .brierScore(0.18)
                .baselineBrierScore(0.24)
                .actualRate(0.45)
                .calibrationJson(calibrationJson)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
