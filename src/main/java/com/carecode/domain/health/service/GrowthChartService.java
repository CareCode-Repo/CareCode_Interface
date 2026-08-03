package com.carecode.domain.health.service;

import com.carecode.core.exception.ChildNotFoundException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.health.dto.response.GrowthPointResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.growth.GrowthMetric;
import com.carecode.domain.health.growth.GrowthPercentileCalculator;
import com.carecode.domain.health.growth.GrowthPercentileResult;
import com.carecode.domain.health.growth.Sex;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 아이 성장 곡선.
 *
 * <p>기록된 키/몸무게를 WHO 성장 표준과 비교해 백분위를 함께 제공한다.
 * 기존 차트 API 는 측정값만 나열해서 "또래와 비교해 어떤지" 를 알 수 없었다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrowthChartService {

    private final ChildRepository childRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final CurrentUserFacade currentUserFacade;

    public List<GrowthPointResponse> getGrowthChart(Long childId, GrowthMetric metric) {
        Child child = requireOwnedChild(childId);

        if (child.getBirthDate() == null) {
            log.warn("생년월일이 없어 백분위를 계산할 수 없습니다. childId={}", childId);
            return List.of();
        }

        Optional<Sex> sex = Sex.parse(child.getGender());
        Function<HealthRecord, Double> valueExtractor = metric == GrowthMetric.WEIGHT
                ? HealthRecord::getWeight
                : HealthRecord::getHeight;

        List<GrowthPointResponse> points = new ArrayList<>();
        List<HealthRecord> records = healthRecordRepository.findByChildOrderByRecordDateDesc(child);

        for (HealthRecord record : records) {
            Double value = valueExtractor.apply(record);
            if (value == null || record.getRecordDate() == null) {
                continue;
            }

            int ageMonths = (int) ChronoUnit.MONTHS.between(child.getBirthDate(), record.getRecordDate());
            GrowthPercentileResult percentile = sex
                    .flatMap(s -> GrowthPercentileCalculator.calculate(metric, s, ageMonths, value))
                    .orElse(null);

            points.add(GrowthPointResponse.of(record.getRecordDate(), ageMonths, value, metric, percentile));
        }

        points.sort(Comparator.comparing(GrowthPointResponse::getRecordDate));
        return points;
    }

    /** 가장 최근 측정치에 대한 백분위 요약. */
    public Optional<GrowthPointResponse> getLatestPercentile(Long childId, GrowthMetric metric) {
        List<GrowthPointResponse> points = getGrowthChart(childId, metric);
        return points.isEmpty() ? Optional.empty() : Optional.of(points.get(points.size() - 1));
    }

    private Child requireOwnedChild(Long childId) {
        User parent = currentUserFacade.requireCurrentUser();
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ChildNotFoundException("아이를 찾을 수 없습니다: " + childId));

        if (child.getUser() == null || !child.getUser().getId().equals(parent.getId())) {
            throw new ChildNotFoundException("아이를 찾을 수 없습니다: " + childId);
        }
        return child;
    }

    /** 오늘 기준 개월 수. */
    public static int ageInMonths(LocalDate birthDate) {
        return (int) ChronoUnit.MONTHS.between(birthDate, LocalDate.now());
    }
}
