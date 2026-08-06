package com.carecode.core.analytics;

import com.carecode.core.analytics.dto.FunnelResponse;
import com.carecode.core.analytics.dto.RetentionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 수집한 이벤트로 퍼널과 리텐션을 계산한다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    /** 온보딩부터 핵심 가치까지의 경로. 순서가 곧 퍼널이다. */
    private static final List<StepDef> FUNNEL = List.of(
            new StepDef(EventType.SIGNED_UP, "가입"),
            new StepDef(EventType.CHILD_REGISTERED, "자녀 등록"),
            new StepDef(EventType.MISSED_BENEFIT_VIEWED, "놓친 지원금 확인"),
            new StepDef(EventType.BENEFIT_LINK_CLICKED, "신청 링크 클릭"));

    /** 알림이 실제로 사람을 돌아오게 하는지. 리텐션의 핵심 지표다. */
    private static final List<StepDef> NOTIFICATION_FUNNEL = List.of(
            new StepDef(EventType.NOTIFICATION_SENT, "알림 발송"),
            new StepDef(EventType.NOTIFICATION_CLICKED, "알림 클릭"),
            new StepDef(EventType.BENEFIT_LINK_CLICKED, "신청 링크 클릭"));

    private static final int MAX_COHORT_DAYS = 60;

    private final UserEventRepository eventRepository;

    private record StepDef(EventType type, String label) {
    }

    public FunnelResponse funnel(LocalDate from, LocalDate to) {
        return buildFunnel(FUNNEL, from, to);
    }

    /** 알림 → 재방문 전환. 이 값이 낮으면 알림 내용이나 시점을 바꿔야 한다. */
    public FunnelResponse notificationFunnel(LocalDate from, LocalDate to) {
        return buildFunnel(NOTIFICATION_FUNNEL, from, to);
    }

    private FunnelResponse buildFunnel(List<StepDef> definition, LocalDate from, LocalDate to) {
        List<FunnelResponse.Step> steps = new ArrayList<>();
        long previous = 0;

        for (int i = 0; i < definition.size(); i++) {
            StepDef def = definition.get(i);
            // 두 번째 단계부터는 앞 단계를 거친 사용자만 센다. 그래야 전환율이 의미를 갖는다.
            long users = i == 0
                    ? eventRepository.countDistinctUsers(def.type(), from, to)
                    : eventRepository.countConverted(definition.get(i - 1).type(), def.type(), from, to);

            steps.add(FunnelResponse.Step.builder()
                    .event(def.type().name())
                    .label(def.label())
                    .users(users)
                    .conversionRate(i == 0 ? null : percentage(users, previous))
                    .build());
            previous = users;
        }

        return FunnelResponse.builder().from(from).to(to).steps(steps).build();
    }

    public RetentionResponse retention(LocalDate from, LocalDate to) {
        LocalDate start = from.isBefore(to.minusDays(MAX_COHORT_DAYS)) ? to.minusDays(MAX_COHORT_DAYS) : from;
        LocalDate today = LocalDate.now();
        List<RetentionResponse.Cohort> cohorts = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(to); date = date.plusDays(1)) {
            List<Long> signedUp = eventRepository.findUserIdsSignedUpOn(date);
            if (signedUp.isEmpty()) {
                continue;
            }
            cohorts.add(RetentionResponse.Cohort.builder()
                    .signUpDate(date)
                    .signedUp(signedUp.size())
                    .day1(retentionAt(signedUp, date, 1, today))
                    .day7(retentionAt(signedUp, date, 7, today))
                    .day30(retentionAt(signedUp, date, 30, today))
                    .build());
        }
        return RetentionResponse.builder().cohorts(cohorts).build();
    }

    /** 아직 그날이 오지 않은 코호트는 0% 가 아니라 미집계다. 구분하지 않으면 지표가 왜곡된다. */
    private Integer retentionAt(List<Long> userIds, LocalDate signUpDate, int offset, LocalDate today) {
        LocalDate target = signUpDate.plusDays(offset);
        if (target.isAfter(today)) {
            return null;
        }
        return percentage(eventRepository.countActiveOn(userIds, target), userIds.size());
    }

    /** 이벤트 종류별 발생 건수. 대시보드 개요용. */
    public Map<String, Long> eventCounts(LocalDate from, LocalDate to) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : eventRepository.countByType(from, to)) {
            counts.put(((EventType) row[0]).name(), (Long) row[1]);
        }
        return counts;
    }

    private Integer percentage(long part, long whole) {
        return whole == 0 ? 0 : (int) Math.round(100.0 * part / whole);
    }
}
