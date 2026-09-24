package com.carecode.domain.careFacility.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
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
import java.util.List;

/**
 * 관측된 정원 변동으로 입소 가능 시점을 추정한다. 통계적 근거가 부족하면 숫자를 만들어내지 않고 부족하다고 답한다.
 *
 * <p>계산은 {@link AdmissionForecastCalculator} 에 있고, 여기서는 관측을 읽어 넘기고 응답을 만든다.
 * 확률과 함께 {@link ForecastAccuracyService} 가 측정한 **같은 확률대의 실제 적중률**을 붙여 내보낸다 —
 * 근거 없는 숫자를 그대로 보여주지 않기 위해서다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmissionForecastService {

    private static final int LOOKBACK_MONTHS = 18;
    private static final int DEFAULT_HORIZON_MONTHS = 6;

    private final CareFacilityRepository careFacilityRepository;
    private final FacilityCapacitySnapshotRepository snapshotRepository;
    private final AdmissionForecastCalculator calculator;
    private final ForecastAccuracyService accuracyService;
    private final EventLogger eventLogger;

    /** 아이 월령 기준으로 목표 시점까지 자리가 날 확률을 추정한다. */
    public AdmissionForecastResponse forecast(Long facilityId, Integer childAgeMonths, Integer horizonMonths) {
        CareFacility facility = careFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareServiceException("시설을 찾을 수 없습니다: " + facilityId));

        LocalDate today = LocalDate.now();
        int horizon = horizonMonths != null && horizonMonths > 0 ? horizonMonths : DEFAULT_HORIZON_MONTHS;

        eventLogger.log(EventType.ADMISSION_FORECAST_VIEWED, null, String.valueOf(facilityId));

        List<FacilityCapacitySnapshot> history =
                snapshotRepository.findHistory(facilityId, today.minusMonths(LOOKBACK_MONTHS));

        AdmissionForecastCalculator.Forecast forecast = calculator.forecast(history, today, horizon);

        AdmissionForecastResponse.AdmissionForecastResponseBuilder response = AdmissionForecastResponse.builder()
                .facilityId(facilityId)
                .facilityName(facility.getName())
                .observationCount(forecast.getObservationCount())
                .observationDays(forecast.getObservationDays())
                .targetClass(resolveClassName(childAgeMonths))
                .targetDate(forecast.getTargetDate())
                .available(forecast.isAvailable())
                .unavailableReason(forecast.getUnavailableReason())
                .probability(forecast.getProbability())
                .confidence(forecast.getConfidence())
                .reasons(forecast.getReasons());

        if (forecast.isAvailable()) {
            response.accuracy(accuracyService.describeFor(horizon, forecast.getProbability()).orElse(null));
        }
        return response.build();
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
