package com.carecode.domain.health.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.domain.facility.entity.CareFacility;
import com.carecode.domain.facility.repository.CareFacilityRepository;
import com.carecode.domain.health.dto.response.HealthAlertResponse;
import com.carecode.domain.health.dto.response.HealthRecommendationResponse;
import com.carecode.domain.health.dto.response.HealthStatsResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.app.ChildDirectory;
import com.carecode.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 건강 기록을 읽어 요약·알림·추천을 만든다.
 *
 * <p>{@link HealthService} 에서 떼어낸 조각이다. 기록을 쓰는 일(생성·수정·삭제·첨부)과 기록을
 * 읽어 해석하는 일은 바뀌는 이유가 다르다 — 앞쪽은 입력 검증과 트랜잭션이, 뒤쪽은 "무엇을
 * 보여줄지" 가 바뀐다. 한 클래스에 있으면 어느 쪽을 고쳐도 다른 쪽 테스트를 함께 본다.
 *
 * <p>여기서는 <b>아무것도 쓰지 않는다.</b> 클래스 전체가 {@code readOnly} 다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthInsightService {

    /** 다가오는 일정을 몇 개까지 보여줄지. 목록이 길면 요약이 아니게 된다. */
    private static final int MAX_UPCOMING_EVENTS = 5;

    /**
     * 알림 우선순위. 지금은 기록만으로 급한지를 판단할 근거가 없어 전부 같은 값이다.
     * 근거(기한 임박, 미접종 차수 등)가 생기면 여기서 나뉜다.
     */
    private static final String DEFAULT_ALERT_PRIORITY = "MEDIUM";

    private final HealthRecordRepository healthRecordRepository;
    private final HealthActorResolver actorResolver;
    private final ChildDirectory childDirectory;
    private final PolicyRepository policyRepository;
    private final CareFacilityRepository careFacilityRepository;

    @LogExecutionTime
    public HealthStatsResponse getHealthStatistics(String userId, Long actorUserId) {
        List<HealthRecord> records = recordsOf(userId, actorUserId);

        return HealthStatsResponse.builder()
                .totalRecords(records.size())
                .completedVaccines(count(records, HealthRecord.RecordType.VACCINATION, true))
                .pendingVaccines(count(records, HealthRecord.RecordType.VACCINATION, false))
                .completedCheckups(count(records, HealthRecord.RecordType.CHECKUP, true))
                .pendingCheckups(count(records, HealthRecord.RecordType.CHECKUP, false))
                .recordTypeDistribution(recordTypeDistribution(records))
                .upcomingEvents(upcomingEvents(records))
                .build();
    }

    /** 다음 방문일이 적혀 있고 아직 오지 않은 기록만 알림으로 본다. */
    @LogExecutionTime
    public List<HealthAlertResponse> getHealthAlerts(String userId, Long actorUserId) {
        LocalDate today = LocalDate.now();

        return recordsOf(userId, actorUserId).stream()
                .filter(record -> record.getNextDate() != null && record.getNextDate().isAfter(today))
                .map(this::toAlert)
                .collect(Collectors.toList());
    }

    /**
     * 아이 연령 기준 연계 추천.
     *
     * <p>연령 단위는 <b>개월</b>이다. 정책의 {@code targetAgeMin/Max} 가 개월이기 때문이다
     * (시드 데이터: "부모급여(0세)" 0~11, "아동수당" 0~95). 한때 여기서만
     * {@code Period.between(...).getYears()} 로 <b>연 나이</b>를 계산해 그 쿼리에 넘겼고,
     * 세 살 아이가 "3" 으로 들어가 <b>0~11개월 대상 정책이 추천됐다.</b>
     *
     * <p>월령을 모르면({@code null}) 연령 기반 조회를 하지 않는다. 전에는 {@code .orElse(0)} 이
     * 0개월로 바꿔, 자녀를 등록하지 않은 사용자에게 신생아 정책을 추천했다.
     */
    @LogExecutionTime
    public HealthRecommendationResponse getIntegratedRecommendations(String userId, Long actorUserId) {
        User user = actorResolver.requireSelf(userId, actorUserId);

        Integer childAgeMonths = childDirectory.childrenOf(user.getId()).stream()
                .map(child -> child.ageMonths(LocalDate.now()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (childAgeMonths == null) {
            return new HealthRecommendationResponse(
                    user.getUserId(), null, List.of(), List.of(),
                    "아이를 등록하면 연령에 맞는 정책과 시설을 알려드립니다.");
        }

        List<String> recommendedPolicies = policyRepository.findByChildAge(childAgeMonths).stream()
                .limit(3)
                .map(Policy::getTitle)
                .collect(Collectors.toList());
        List<String> recommendedFacilities = careFacilityRepository.findByChildAge(childAgeMonths).stream()
                .limit(3)
                .map(CareFacility::getName)
                .collect(Collectors.toList());

        return new HealthRecommendationResponse(
                user.getUserId(),
                childAgeMonths,
                recommendedPolicies,
                recommendedFacilities,
                "아이 연령에 맞는 정책/시설을 확인해보세요.");
    }

    /** 본인 확인과 조회를 한 번에. JOIN FETCH 로 Child·User 를 함께 읽어 N+1 을 피한다. */
    private List<HealthRecord> recordsOf(String userId, Long actorUserId) {
        User user = actorResolver.requireSelf(userId, actorUserId);
        return healthRecordRepository.findByUserIdWithChildAndUser(user.getId());
    }

    private int count(List<HealthRecord> records, HealthRecord.RecordType type, boolean completed) {
        return (int) records.stream()
                .filter(record -> record.getRecordType() == type)
                .filter(record -> Boolean.TRUE.equals(record.getIsCompleted()) == completed)
                .count();
    }

    private Map<String, Integer> recordTypeDistribution(List<HealthRecord> records) {
        return records.stream()
                .filter(record -> record.getRecordType() != null)
                .collect(Collectors.groupingBy(
                        record -> record.getRecordType().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    private List<String> upcomingEvents(List<HealthRecord> records) {
        LocalDate today = LocalDate.now();

        return records.stream()
                .filter(record -> record.getNextDate() != null && record.getNextDate().isAfter(today))
                .map(record -> String.format("%s: %s", record.getTitle(), record.getNextDate()))
                .limit(MAX_UPCOMING_EVENTS)
                .collect(Collectors.toList());
    }

    private HealthAlertResponse toAlert(HealthRecord record) {
        return HealthAlertResponse.builder()
                .alertId(record.getId().toString())
                .alertType(record.getRecordType() != null ? record.getRecordType().name() : null)
                .title(record.getTitle())
                .message(record.getDescription())
                .priority(DEFAULT_ALERT_PRIORITY)
                .dueDate(record.getNextDate() != null ? record.getNextDate().toString() : null)
                .isRead(false)
                .build();
    }
}
