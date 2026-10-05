package com.carecode.domain.facility.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.core.exception.BusinessException;
import com.carecode.domain.facility.dto.response.AdmissionCandidateResponse;
import com.carecode.domain.facility.entity.CareFacility;
import com.carecode.domain.facility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.facility.repository.CareFacilityRepository;
import com.carecode.domain.facility.repository.FacilityCapacitySnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 지역 안의 시설을 아이 기준 입소 확률 순으로 돌려준다.
 *
 * <p>{@link AdmissionForecastService} 는 "이 시설에 들어갈 수 있나" 에 답한다. 그런데 그 질문을 하려면
 * 먼저 시설을 골라야 하고, 어디를 고를지 모르는 사람은 확률을 영영 보지 못한다. 여기서는 질문을 뒤집어
 * "어디에 들어갈 수 있나" 에 답한다.
 *
 * <p>계산은 같은 {@link AdmissionForecastCalculator} 를 쓴다. 시설 상세에서 본 확률과 목록에서 본 확률이
 * 다르면 둘 다 믿을 수 없게 되기 때문이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmissionCandidateService {

    /** 예측 입력으로 삼는 관측 구간. {@link AdmissionForecastService} 와 같아야 같은 답이 나온다. */
    private static final int LOOKBACK_MONTHS = 18;
    private static final int DEFAULT_HORIZON_MONTHS = 6;

    /**
     * 한 번에 확률을 계산할 시설 수 상한.
     *
     * <p>구 단위로 어린이집이 수백 곳이다. 전부 계산해도 사용자가 보는 건 위에서 열 곳 남짓이라
     * 여기서 끊는다. 끊었다는 사실은 evaluatedFacilities 로 드러난다.
     */
    private static final int MAX_EVALUATED = 200;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final CareFacilityRepository facilityRepository;
    private final FacilityCapacitySnapshotRepository snapshotRepository;
    private final AdmissionForecastCalculator calculator;
    private final ForecastAccuracyService accuracyService;
    private final EventLogger eventLogger;

    public AdmissionCandidateResponse recommend(String region, Integer childAgeMonths,
                                                Integer horizonMonths, Integer limit) {
        // BusinessException(= ErrorCode.INVALID_INPUT) 이라야 400 이 나간다. CareServiceException 은
        // 기본 매핑이 500 이어서, 잘못 보낸 요청에 서버 오류와 운영 알림이 함께 나갔다.
        if (region == null || region.isBlank()) {
            throw new BusinessException("지역을 입력해주세요.");
        }
        if (childAgeMonths == null || childAgeMonths < 0) {
            throw new BusinessException("아이 월령을 입력해주세요.");
        }

        LocalDate today = LocalDate.now();
        int horizon = horizonMonths != null && horizonMonths > 0 ? horizonMonths : DEFAULT_HORIZON_MONTHS;
        int size = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;

        List<CareFacility> facilities = facilityRepository.findAdmissionCandidates(
                region.trim(), childAgeMonths / 12, PageRequest.of(0, MAX_EVALUATED));

        AdmissionCandidateResponse.AdmissionCandidateResponseBuilder response =
                AdmissionCandidateResponse.builder()
                        .region(region.trim())
                        .childAgeMonths(childAgeMonths)
                        .targetClass(AdmissionForecastCalculator.resolveClassName(childAgeMonths))
                        .horizonMonths(horizon)
                        .targetDate(today.plusMonths(horizon))
                        .evaluatedFacilities(facilities.size())
                        .accuracy(accuracyService.describeFor(horizon, null).orElse(null));

        if (facilities.isEmpty()) {
            return response.notEnoughDataCount(0).candidates(List.of()).build();
        }

        eventLogger.log(EventType.ADMISSION_FORECAST_VIEWED, null, region.trim());

        Map<Long, List<FacilityCapacitySnapshot>> histories = loadHistories(facilities, today);

        List<AdmissionCandidateResponse.Candidate> candidates = new ArrayList<>();
        int notEnoughData = 0;

        for (CareFacility facility : facilities) {
            List<FacilityCapacitySnapshot> history =
                    histories.getOrDefault(facility.getId(), List.of());
            AdmissionForecastCalculator.Forecast forecast = calculator.forecast(history, today, horizon);

            // 관측이 모자란 곳은 숫자를 지어내지 않고 센다. 지어낸 확률이 가장 해롭다.
            if (!forecast.isAvailable() || forecast.getProbability() == null) {
                notEnoughData++;
                continue;
            }

            candidates.add(AdmissionCandidateResponse.Candidate.builder()
                    .facilityId(facility.getId())
                    .facilityName(facility.getName())
                    .address(facility.getAddress())
                    .probability(forecast.getProbability())
                    .confidence(forecast.getConfidence())
                    .observationCount(forecast.getObservationCount())
                    .observationDays(forecast.getObservationDays())
                    .availableSpots(facility.getAvailableSpots())
                    .reasons(forecast.getReasons())
                    .build());
        }

        // 확률이 같으면 관측이 많은 쪽이 먼저다. 같은 숫자라도 근거가 두꺼운 편을 위에 둔다.
        candidates.sort(Comparator
                .comparing(AdmissionCandidateResponse.Candidate::getProbability).reversed()
                .thenComparing(Comparator.comparingLong(
                        AdmissionCandidateResponse.Candidate::getObservationCount).reversed()));

        return response
                .notEnoughDataCount(notEnoughData)
                .candidates(candidates.size() > size ? candidates.subList(0, size) : candidates)
                .build();
    }

    /** 후보 전체의 관측을 한 번에 읽어 시설별로 나눈다. 시설마다 묻으면 후보 수만큼 왕복한다. */
    private Map<Long, List<FacilityCapacitySnapshot>> loadHistories(List<CareFacility> facilities,
                                                                    LocalDate today) {
        List<Long> ids = facilities.stream().map(CareFacility::getId).toList();
        return snapshotRepository.findHistories(ids, today.minusMonths(LOOKBACK_MONTHS)).stream()
                .collect(Collectors.groupingBy(FacilityCapacitySnapshot::getFacilityId));
    }
}
