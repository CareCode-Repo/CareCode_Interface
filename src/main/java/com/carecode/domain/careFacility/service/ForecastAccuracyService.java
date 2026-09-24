package com.carecode.domain.careFacility.service;

import com.carecode.domain.careFacility.dto.response.ForecastAccuracyResponse;
import com.carecode.domain.careFacility.entity.ForecastAccuracy;
import com.carecode.domain.careFacility.repository.ForecastAccuracyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 측정된 예측 정확도를 읽어 응답으로 만든다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForecastAccuracyService {

    /** 이보다 표본이 적으면 정확도라고 부를 수 없어 아예 내보내지 않는다. */
    static final int MIN_SAMPLES = 30;

    /** 구간별 적중률은 구간 표본이 이만큼은 있어야 보여준다. */
    static final int MIN_BUCKET_SAMPLES = 10;

    private final ForecastAccuracyRepository accuracyRepository;
    private final ObjectMapper objectMapper;

    /** 특정 기간·확률에 대한 정확도. 표본이 부족하면 빈 값(화면에서 숨긴다). */
    public Optional<ForecastAccuracyResponse> describeFor(int horizonMonths, Integer probability) {
        return accuracyRepository.findFirstByHorizonMonthsOrderByRunDateDesc(horizonMonths)
                .filter(accuracy -> accuracy.getSamples() >= MIN_SAMPLES)
                .map(accuracy -> toResponse(accuracy, probability, false));
    }

    /** 공개 통계용. 측정된 모든 기간의 최신 결과를 구간표까지 함께 준다. */
    public List<ForecastAccuracyResponse> latestByHorizon(List<Integer> horizons) {
        List<ForecastAccuracyResponse> results = new ArrayList<>();
        for (int horizon : horizons) {
            accuracyRepository.findFirstByHorizonMonthsOrderByRunDateDesc(horizon)
                    .map(accuracy -> toResponse(accuracy, null, true))
                    .ifPresent(results::add);
        }
        return results;
    }

    private ForecastAccuracyResponse toResponse(ForecastAccuracy accuracy, Integer probability, boolean includeAll) {
        List<ForecastBacktestService.Bucket> buckets = readBuckets(accuracy);
        List<ForecastAccuracyResponse.Bucket> mapped = buckets.stream().map(ForecastAccuracyService::toBucket).toList();

        ForecastAccuracyResponse.Bucket matched = null;
        if (probability != null) {
            matched = mapped.stream()
                    .filter(b -> probability >= b.getFrom() && (probability < b.getTo() || b.getTo() == 100))
                    .filter(b -> b.getSamples() >= MIN_BUCKET_SAMPLES)
                    .findFirst()
                    .orElse(null);
        }

        return ForecastAccuracyResponse.builder()
                .measuredAt(accuracy.getRunDate())
                .horizonMonths(accuracy.getHorizonMonths())
                .samples(accuracy.getSamples())
                .facilities(accuracy.getFacilities())
                .actualRate(accuracy.getActualRate())
                .brierScore(accuracy.getBrierScore())
                .baselineBrierScore(accuracy.getBaselineBrierScore())
                .betterThanBaseline(accuracy.betterThanBaseline())
                .matchedBucket(matched)
                .calibration(includeAll ? mapped : null)
                .build();
    }

    private List<ForecastBacktestService.Bucket> readBuckets(ForecastAccuracy accuracy) {
        try {
            return objectMapper.readValue(accuracy.getCalibrationJson(),
                    new TypeReference<List<ForecastBacktestService.Bucket>>() {
                    });
        } catch (Exception e) {
            // 예전 기록의 구조가 달라졌을 수 있다. 전체 지표는 살리고 구간표만 비운다.
            log.warn("정확도 구간표를 읽지 못했습니다 - id={}", accuracy.getId(), e);
            return List.of();
        }
    }

    private static ForecastAccuracyResponse.Bucket toBucket(ForecastBacktestService.Bucket bucket) {
        return ForecastAccuracyResponse.Bucket.builder()
                .from(bucket.from())
                .to(bucket.to())
                .samples(bucket.samples())
                .actualTrue(bucket.actualTrue())
                .actualRate(bucket.actualRate())
                .build();
    }
}
