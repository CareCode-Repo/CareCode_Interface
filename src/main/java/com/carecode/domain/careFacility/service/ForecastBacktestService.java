package com.carecode.domain.careFacility.service;

import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.entity.ForecastAccuracy;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import com.carecode.domain.careFacility.repository.ForecastAccuracyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 예측이 맞았는지 과거 데이터로 확인한다 (백테스트).
 *
 * <p>확률을 보여주면서 그 확률이 얼마나 맞는지 모르는 상태였다. 실제 예측을 기록해 몇 달을 기다리는 대신,
 * 이미 쌓인 정원 관측 시계열로 과거 시점을 재현한다: 기준일 이전 관측만 넘겨 그때의 예측을 다시 계산하고,
 * 기준일 다음 구간의 관측으로 실제 결과를 확인한다. 라이브 예측과 **같은 계산기**를 쓴다
 * ({@link AdmissionForecastCalculator}) — 다른 코드로 검증하면 의미가 없다.
 *
 * <p>측정 결과는 {@code TBL_FORECAST_ACCURACY} 에 기간별로 남기고, 화면에는 확률과 함께
 * 같은 확률대의 실제 적중률을 붙여 보여준다.
 */
@Slf4j
@Service
public class ForecastBacktestService {

    /**
     * 정확도를 측정·공개하는 예측 기간(개월).
     *
     * <p>6개월만 측정하면 관측이 6개월 이상 쌓인 시설이 있어야 표본이 생긴다. 수집을 시작한 지 얼마
     * 안 된 상태에서도 검증이 돌아가도록 짧은 기간을 함께 둔다.
     */
    public static final List<Integer> MEASURED_HORIZONS = List.of(1, 3, 6);

    /** 검증 대상이 되려면 이 정도 관측은 있어야 한다. */
    private static final int MIN_SNAPSHOTS_PER_FACILITY = 6;

    /** 기준일을 이 간격으로 옮기며 예측을 만든다. 더 촘촘하게 하면 같은 구간을 겹쳐 세게 된다. */
    private static final int STEP_DAYS = 30;

    /** 확률대 구간 폭(%). */
    private static final int BUCKET_WIDTH = 20;

    private final FacilityCapacitySnapshotRepository snapshotRepository;
    private final ForecastAccuracyRepository accuracyRepository;
    private final AdmissionForecastCalculator calculator;
    private final ObjectMapper objectMapper;

    /** 주간 작업 시간을 묶기 위한 상한. 정확도는 표본 추정이라 전수를 볼 필요가 없다. */
    private final int maxFacilities;

    public ForecastBacktestService(FacilityCapacitySnapshotRepository snapshotRepository,
                                  ForecastAccuracyRepository accuracyRepository,
                                  AdmissionForecastCalculator calculator,
                                  ObjectMapper objectMapper,
                                  @org.springframework.beans.factory.annotation.Value(
                                          "${app.forecast.backtest.max-facilities:2000}") int maxFacilities) {
        this.snapshotRepository = snapshotRepository;
        this.accuracyRepository = accuracyRepository;
        this.calculator = calculator;
        this.objectMapper = objectMapper;
        this.maxFacilities = maxFacilities;
    }

    /** 기본 기간 전부 측정. */
    public List<ForecastAccuracy> runAll() {
        return runAll(MEASURED_HORIZONS);
    }

    /**
     * 기간별로 백테스트를 돌려 결과를 저장한다. 표본이 없는 기간은 저장하지 않는다.
     *
     * <p>시설 이력은 시설당 한 번만 읽고 모든 기간에 재사용한다. 기간마다 다시 읽으면
     * 전국 규모(수만 곳)에서 주간 작업이 쿼리 수만 건으로 늘어난다.
     */
    @Transactional
    public List<ForecastAccuracy> runAll(List<Integer> horizonMonths) {
        List<Long> facilityIds = snapshotRepository.findFacilityIdsWithAtLeast(MIN_SNAPSHOTS_PER_FACILITY);
        if (facilityIds.size() > maxFacilities) {
            // 정확도는 표본 추정이므로 전수를 보지 않아도 된다. 주간 작업 시간을 묶어 두는 편이 낫다.
            log.info("백테스트 대상 시설이 {}곳이라 앞에서 {}곳만 본다", facilityIds.size(), maxFacilities);
            facilityIds = facilityIds.subList(0, maxFacilities);
        }
        log.info("예측 정확도 측정 시작 - 대상 시설 {}곳, 기간 {}", facilityIds.size(), horizonMonths);

        Map<Integer, List<Sample>> samplesByHorizon = new LinkedHashMap<>();
        Map<Integer, Integer> facilitiesByHorizon = new LinkedHashMap<>();
        for (int horizon : horizonMonths) {
            samplesByHorizon.put(horizon, new ArrayList<>());
            facilitiesByHorizon.put(horizon, 0);
        }

        for (Long facilityId : facilityIds) {
            List<FacilityCapacitySnapshot> history =
                    snapshotRepository.findHistory(facilityId, LocalDate.of(2000, 1, 1));
            for (int horizon : horizonMonths) {
                List<Sample> samples = sampleFacility(history, horizon);
                if (!samples.isEmpty()) {
                    samplesByHorizon.get(horizon).addAll(samples);
                    facilitiesByHorizon.merge(horizon, 1, Integer::sum);
                }
            }
        }

        List<ForecastAccuracy> saved = new ArrayList<>();
        for (int horizon : horizonMonths) {
            summarize(horizon, samplesByHorizon.get(horizon), facilitiesByHorizon.get(horizon))
                    .ifPresent(result -> saved.add(accuracyRepository.save(result)));
        }
        return saved;
    }

    private Optional<ForecastAccuracy> summarize(int horizon, List<Sample> samples, int facilitiesUsed) {
        if (samples.isEmpty()) {
            log.info("예측 정확도 측정 - {}개월: 검증 가능한 표본이 없습니다(관측 기간이 예측 기간보다 짧음)", horizon);
            return Optional.empty();
        }

        double actualRate = samples.stream().filter(Sample::actual).count() / (double) samples.size();
        double brier = samples.stream()
                .mapToDouble(s -> Math.pow(s.probability() / 100.0 - (s.actual() ? 1 : 0), 2))
                .average().orElse(0);
        // 비교 기준: 예측하지 않고 항상 평균 발생률로 답했을 때. 이보다 못하면 확률을 보여줄 근거가 없다.
        double baselineBrier = samples.stream()
                .mapToDouble(s -> Math.pow(actualRate - (s.actual() ? 1 : 0), 2))
                .average().orElse(0);

        ForecastAccuracy result = ForecastAccuracy.builder()
                .runDate(LocalDate.now())
                .horizonMonths(horizon)
                .samples(samples.size())
                .facilities(facilitiesUsed)
                .brierScore(round(brier))
                .baselineBrierScore(round(baselineBrier))
                .actualRate(round(actualRate))
                .calibrationJson(toJson(calibrate(samples)))
                .createdAt(LocalDateTime.now())
                .build();

        log.info("예측 정확도 측정 - {}개월: 표본 {}건, 실제 발생률 {}, Brier {} (기준 {})",
                horizon, samples.size(), result.getActualRate(), result.getBrierScore(), result.getBaselineBrierScore());
        return Optional.of(result);
    }

    /**
     * 한 시설에서 검증 가능한 (예측, 실제) 쌍을 모은다.
     *
     * <p>기준일 이후 관측은 예측 입력에서 제외한다. 넣으면 미래를 보고 예측하는 셈이라 정확도가 부풀려진다.
     * 검증 구간에 관측이 아예 없으면 실제 결과를 알 수 없으므로 표본에서 버린다.
     */
    private List<Sample> sampleFacility(List<FacilityCapacitySnapshot> history, int horizon) {
        List<Sample> samples = new ArrayList<>();
        if (history.size() < MIN_SNAPSHOTS_PER_FACILITY) {
            return samples;
        }

        LocalDate first = history.get(0).getObservedDate();
        LocalDate last = history.get(history.size() - 1).getObservedDate();
        LocalDate asOf = first.plusDays(AdmissionForecastCalculator.MIN_OBSERVATION_DAYS);

        while (!asOf.plusMonths(horizon).isAfter(last)) {
            LocalDate cutoff = asOf;
            List<FacilityCapacitySnapshot> past = history.stream()
                    .filter(s -> !s.getObservedDate().isAfter(cutoff))
                    .toList();
            List<FacilityCapacitySnapshot> future = history.stream()
                    .filter(s -> s.getObservedDate().isAfter(cutoff)
                            && !s.getObservedDate().isAfter(cutoff.plusMonths(horizon)))
                    .toList();

            AdmissionForecastCalculator.Forecast forecast = calculator.forecast(past, asOf, horizon);
            if (forecast.isAvailable() && !future.isEmpty()) {
                boolean actual = future.stream().anyMatch(AdmissionForecastCalculator::hasSeat);
                samples.add(new Sample(forecast.getProbability(), actual));
            }
            asOf = asOf.plusDays(STEP_DAYS);
        }
        return samples;
    }

    /** 확률대별로 "그렇게 예측한 건 중 실제로 자리가 난 비율". 예측이 정직한지 보여주는 표다. */
    private List<Bucket> calibrate(List<Sample> samples) {
        List<Bucket> buckets = new ArrayList<>();
        for (int from = 0; from < 100; from += BUCKET_WIDTH) {
            int to = from + BUCKET_WIDTH;
            int lower = from;
            List<Sample> inBucket = samples.stream()
                    .filter(s -> s.probability() >= lower && (s.probability() < to || (to == 100 && s.probability() == 100)))
                    .toList();
            buckets.add(new Bucket(from, to, inBucket.size(),
                    (int) inBucket.stream().filter(Sample::actual).count()));
        }
        return buckets;
    }

    private String toJson(List<Bucket> buckets) {
        try {
            return objectMapper.writeValueAsString(buckets);
        } catch (JsonProcessingException e) {
            // 구조가 고정된 값이라 실패할 이유가 없지만, 실패하면 측정 자체를 버리는 편이 낫다.
            throw new IllegalStateException("정확도 구간 직렬화 실패", e);
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000) / 10000.0;
    }

    /** 예측 한 건과 그 결과. */
    private record Sample(int probability, boolean actual) {
    }

    /** 확률대 구간. JSON 으로 저장되므로 필드 이름을 바꾸면 예전 기록을 읽을 수 없다. */
    public record Bucket(int from, int to, int samples, int actualTrue) {
        public Double actualRate() {
            return samples == 0 ? null : Math.round(actualTrue * 1000.0 / samples) / 1000.0;
        }
    }
}
