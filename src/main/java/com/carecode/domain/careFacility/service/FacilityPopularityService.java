package com.carecode.domain.careFacility.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.careFacility.dto.response.FacilityPopularityResponse;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 충원율 추이로 시설 인기도를 추정한다.
 * 평가인증은 대부분 최고등급이라 변별력이 없고 리뷰는 조작될 수 있지만,
 * 충원율은 공공데이터가 원천이라 시설이 개입할 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityPopularityService {

    private static final int MIN_OBSERVATIONS = 4;
    private static final int LOOKBACK_MONTHS = 24;

    /** 이 이상이면 사실상 만원으로 본다. 정원 관리 여유를 감안한 값이다. */
    private static final int FULL_THRESHOLD = 98;

    private static final int IN_DEMAND_THRESHOLD = 90;
    private static final int UNDERSUBSCRIBED_THRESHOLD = 70;

    /** 충원율이 이만큼 떨어지면 운영 변화 신호로 본다. */
    private static final int SHARP_DROP_POINTS = 15;

    /** 추세 판정 기준. 전·후반 평균 차이. */
    private static final int TREND_POINTS = 5;

    private final CareFacilityRepository careFacilityRepository;
    private final FacilityCapacitySnapshotRepository snapshotRepository;

    public FacilityPopularityResponse analyze(Long facilityId) {
        CareFacility facility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareServiceException("시설을 찾을 수 없습니다: " + facilityId));

        List<FacilityCapacitySnapshot> history =
                snapshotRepository.findHistory(facilityId, LocalDate.now().minusMonths(LOOKBACK_MONTHS));

        List<Rate> rates = toFillRates(history);

        FacilityPopularityResponse.FacilityPopularityResponseBuilder base = FacilityPopularityResponse.builder()
                .facilityId(facilityId)
                .facilityName(facility.getName())
                .observationCount(rates.size());

        if (rates.size() < MIN_OBSERVATIONS) {
            return base.available(false)
                    .unavailableReason(String.format("정원 관측이 %d회로 부족합니다. (최소 %d회 필요)",
                            rates.size(), MIN_OBSERVATIONS))
                    .build();
        }

        int average = (int) Math.round(rates.stream().mapToInt(Rate::fillRate).average().orElse(0));
        int latest = rates.get(rates.size() - 1).fillRate();
        int fullRatio = (int) Math.round(
                100.0 * rates.stream().filter(r -> r.fillRate() >= FULL_THRESHOLD).count() / rates.size());

        String trend = resolveTrend(rates);
        String demandLevel = resolveDemandLevel(average, fullRatio);
        List<LocalDate> drops = findSharpDrops(rates);

        return base.available(true)
                .averageFillRate(average)
                .latestFillRate(latest)
                .fullRatio(fullRatio)
                .trend(trend)
                .demandLevel(demandLevel)
                .sharpDropDates(drops)
                .reasons(buildReasons(average, fullRatio, trend, demandLevel, drops))
                .build();
    }

    private record Rate(LocalDate date, int fillRate) {
    }

    /** 정원이 0 이거나 없는 관측은 비율을 낼 수 없어 버린다. */
    private List<Rate> toFillRates(List<FacilityCapacitySnapshot> history) {
        List<Rate> rates = new ArrayList<>();
        for (FacilityCapacitySnapshot s : history) {
            Integer capacity = s.getCapacity();
            Integer enrolled = s.getCurrentEnrollment();
            if (capacity == null || capacity <= 0 || enrolled == null) {
                continue;
            }
            rates.add(new Rate(s.getObservedDate(), (int) Math.round(100.0 * enrolled / capacity)));
        }
        return rates;
    }

    /** 전반부와 후반부 평균을 비교한다. 관측 간격이 불규칙해 회귀보다 이쪽이 안정적이다. */
    private String resolveTrend(List<Rate> rates) {
        int half = rates.size() / 2;
        double earlier = rates.subList(0, half).stream().mapToInt(Rate::fillRate).average().orElse(0);
        double later = rates.subList(half, rates.size()).stream().mapToInt(Rate::fillRate).average().orElse(0);

        double delta = later - earlier;
        if (delta >= TREND_POINTS) {
            return "RISING";
        }
        return delta <= -TREND_POINTS ? "FALLING" : "STABLE";
    }

    private String resolveDemandLevel(int average, int fullRatio) {
        if (average >= IN_DEMAND_THRESHOLD || fullRatio >= 50) {
            return "IN_DEMAND";
        }
        return average < UNDERSUBSCRIBED_THRESHOLD ? "UNDERSUBSCRIBED" : "STEADY";
    }

    /** 직전 관측 대비 급락 지점. 3월 신학기 전환은 정상 변동이라 제외한다. */
    private List<LocalDate> findSharpDrops(List<Rate> rates) {
        List<LocalDate> drops = new ArrayList<>();
        for (int i = 1; i < rates.size(); i++) {
            Rate current = rates.get(i);
            int delta = current.fillRate() - rates.get(i - 1).fillRate();
            if (delta <= -SHARP_DROP_POINTS && current.date().getMonthValue() != 3) {
                drops.add(current.date());
            }
        }
        return drops;
    }

    private List<String> buildReasons(int average, int fullRatio, String trend,
                                      String demandLevel, List<LocalDate> drops) {
        List<String> reasons = new ArrayList<>();
        reasons.add(String.format("평균 충원율 %d%%, 관측 중 %d%% 기간이 정원에 도달했습니다.", average, fullRatio));

        switch (demandLevel) {
            case "IN_DEMAND" -> reasons.add("정원이 자주 차는 시설로, 대기가 길 수 있습니다.");
            case "UNDERSUBSCRIBED" -> reasons.add("정원에 여유가 지속되고 있어 입소가 비교적 쉽습니다.");
            default -> reasons.add("정원과 현원이 안정적으로 유지되고 있습니다.");
        }

        switch (trend) {
            case "RISING" -> reasons.add("충원율이 상승 추세입니다.");
            case "FALLING" -> reasons.add("충원율이 하락 추세입니다.");
            default -> { }
        }

        if (!drops.isEmpty()) {
            reasons.add(String.format("충원율이 크게 떨어진 시점이 %d회 있었습니다. 신학기 전환이 아닌 변동입니다.",
                    drops.size()));
        }
        return reasons;
    }
}
