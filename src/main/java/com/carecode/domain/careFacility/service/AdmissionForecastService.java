package com.carecode.domain.careFacility.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.careFacility.dto.response.AdmissionForecastResponse;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 관측된 정원 변동으로 입소 가능 시점을 추정한다.
 * 통계적 근거가 부족하면 숫자를 만들어내지 않고 부족하다고 답한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmissionForecastService {

    /** 이보다 관측이 적으면 추세라고 부를 수 없다. */
    private static final int MIN_OBSERVATIONS = 4;
    private static final int MIN_OBSERVATION_DAYS = 60;

    /** 신학기. 승급·졸업으로 자리가 가장 많이 열리는 시점이다. */
    private static final Month NEW_TERM_MONTH = Month.MARCH;

    private static final int LOOKBACK_MONTHS = 18;
    private static final int DEFAULT_HORIZON_MONTHS = 6;

    private final CareFacilityRepository careFacilityRepository;
    private final FacilityCapacitySnapshotRepository snapshotRepository;

    /** 아이 월령 기준으로 목표 시점까지 자리가 날 확률을 추정한다. */
    public AdmissionForecastResponse forecast(Long facilityId, Integer childAgeMonths, Integer horizonMonths) {
        CareFacility facility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareServiceException("시설을 찾을 수 없습니다: " + facilityId));

        LocalDate today = LocalDate.now();
        int horizon = horizonMonths != null && horizonMonths > 0 ? horizonMonths : DEFAULT_HORIZON_MONTHS;
        LocalDate targetDate = today.plusMonths(horizon);

        List<FacilityCapacitySnapshot> history =
                snapshotRepository.findHistory(facilityId, today.minusMonths(LOOKBACK_MONTHS));

        long observationDays = history.isEmpty() ? 0
                : ChronoUnit.DAYS.between(history.get(0).getObservedDate(), today);

        AdmissionForecastResponse.AdmissionForecastResponseBuilder base = AdmissionForecastResponse.builder()
                .facilityId(facilityId)
                .facilityName(facility.getName())
                .observationCount(history.size())
                .observationDays(observationDays)
                .targetClass(resolveClassName(childAgeMonths))
                .targetDate(targetDate);

        String shortage = checkDataSufficiency(history.size(), observationDays);
        if (shortage != null) {
            return base.available(false).unavailableReason(shortage).build();
        }

        return buildForecast(base, history, targetDate, today);
    }

    /** 관측이 부족한 이유를 사용자가 이해할 수 있게 돌려준다. */
    private String checkDataSufficiency(int count, long days) {
        if (count == 0) {
            return "이 시설의 정원 관측 이력이 아직 없습니다.";
        }
        if (count < MIN_OBSERVATIONS) {
            return String.format("관측 %d회로는 추세를 판단할 수 없습니다. (최소 %d회 필요)", count, MIN_OBSERVATIONS);
        }
        if (days < MIN_OBSERVATION_DAYS) {
            return String.format("관측 기간이 %d일로 짧습니다. (최소 %d일 필요)", days, MIN_OBSERVATION_DAYS);
        }
        return null;
    }

    private AdmissionForecastResponse buildForecast(
            AdmissionForecastResponse.AdmissionForecastResponseBuilder base,
            List<FacilityCapacitySnapshot> history, LocalDate targetDate, LocalDate today) {

        List<String> reasons = new ArrayList<>();

        // 1. 관측 구간 중 자리가 있었던 비율 — 예측의 기준선
        long observedWithSeat = history.stream().filter(this::hasSeat).count();
        double baseRate = (double) observedWithSeat / history.size();
        reasons.add(String.format("최근 관측 %d회 중 %d회에 잔여석이 있었습니다.", history.size(), observedWithSeat));

        // 2. 자리가 실제로 열린 횟수. 잔여석이 늘어난 전환만 센다.
        int openings = countSeatOpenings(history);
        double openingsPerMonth = monthsCovered(history) > 0 ? openings / monthsCovered(history) : 0;
        if (openings > 0) {
            reasons.add(String.format("관측 기간에 자리가 %d회 열렸습니다 (월 평균 %.1f회).", openings, openingsPerMonth));
        } else {
            reasons.add("관측 기간에 자리가 열린 적이 없습니다.");
        }

        // 3. 목표 시점까지 신학기가 끼면 승급·졸업으로 자리가 크게 열린다.
        boolean spansNewTerm = spansNewTerm(today, targetDate);
        if (spansNewTerm) {
            reasons.add("목표 시점까지 3월 신학기가 포함되어 승급·졸업으로 자리가 열릴 가능성이 높습니다.");
        }

        int probability = estimateProbability(baseRate, openingsPerMonth,
                ChronoUnit.MONTHS.between(today, targetDate), spansNewTerm);

        return base.available(true)
                .probability(probability)
                .confidence(resolveConfidence(history.size(), openings))
                .reasons(reasons)
                .build();
    }

    /**
     * 기준선(관측 중 자리 있던 비율)에 자리 발생률을 포아송으로 얹는다.
     * 정교한 모델이 아니라 관측을 그대로 반영하는 추정치이며, 근거를 함께 노출해 과신을 막는다.
     */
    private int estimateProbability(double baseRate, double openingsPerMonth, long months, boolean spansNewTerm) {
        // 기간 내 자리가 최소 1회 열릴 확률 = 1 - e^(-λt)
        double openingProbability = 1 - Math.exp(-openingsPerMonth * Math.max(months, 1));
        double combined = 1 - (1 - baseRate) * (1 - openingProbability);

        if (spansNewTerm) {
            // 신학기는 관측만으로 잡히지 않는 구조적 요인이라 하한을 둔다.
            combined = Math.max(combined, 0.5);
        }
        return (int) Math.round(Math.min(combined, 0.95) * 100);
    }

    /** 잔여석이 0 이하에서 1 이상으로 바뀐 전환 횟수. */
    private int countSeatOpenings(List<FacilityCapacitySnapshot> history) {
        int openings = 0;
        boolean previousHadSeat = hasSeat(history.get(0));
        for (int i = 1; i < history.size(); i++) {
            boolean current = hasSeat(history.get(i));
            if (current && !previousHadSeat) {
                openings++;
            }
            previousHadSeat = current;
        }
        return openings;
    }

    private boolean hasSeat(FacilityCapacitySnapshot snapshot) {
        Integer spots = snapshot.getAvailableSpots();
        if (spots != null) {
            return spots > 0;
        }
        Integer capacity = snapshot.getCapacity();
        Integer enrolled = snapshot.getCurrentEnrollment();
        return capacity != null && enrolled != null && capacity > enrolled;
    }

    private double monthsCovered(List<FacilityCapacitySnapshot> history) {
        long days = ChronoUnit.DAYS.between(
                history.get(0).getObservedDate(), history.get(history.size() - 1).getObservedDate());
        return days / 30.0;
    }

    private boolean spansNewTerm(LocalDate from, LocalDate to) {
        LocalDate term = LocalDate.of(from.getYear(), NEW_TERM_MONTH, 1);
        if (term.isBefore(from)) {
            term = term.plusYears(1);
        }
        return !term.isAfter(to);
    }

    /** 관측이 많고 자리 열림이 실제로 관측됐을수록 신뢰도가 높다. */
    private String resolveConfidence(int observations, int openings) {
        if (observations >= 24 && openings >= 3) {
            return "HIGH";
        }
        if (observations >= 12 && openings >= 1) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /** 어린이집 반 편성은 만 나이 기준이다. */
    private String resolveClassName(Integer ageMonths) {
        if (ageMonths == null) {
            return null;
        }
        int years = ageMonths / 12;
        return years >= 5 ? "5세반 이상" : years + "세반";
    }
}
