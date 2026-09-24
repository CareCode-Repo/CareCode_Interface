package com.carecode.domain.health.timeline;

import com.carecode.domain.health.dto.response.ChildTimelineResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.entity.VaccinationSchedule;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.health.repository.VaccinationScheduleRepository;
import com.carecode.domain.health.service.ChildService;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.Child;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 아이 한 명의 할 일을 시간 축 하나로 합친다.
 *
 * <p>접종·검진·지원금 마감이 화면 세 곳에 흩어져 있어서, 부모가 "다음에 뭘 해야 하나" 를 알려면
 * 세 곳을 돌아야 했다. 새로 수집하는 데이터는 없다 — 이미 있는 것을 한 축에 놓을 뿐이다.
 *
 * <p>담는 것과 담지 않는 것:
 * <ul>
 *   <li>접종 — 자동 생성된 표준 일정. 완료된 건 제외하고 놓친 건 구간 앞이라도 포함한다.</li>
 *   <li>검진 — 국가 영유아 건강검진 권장 시기({@link CheckupStandard}). 완료 여부는 그 구간에 남은
 *       검진 기록으로 판단한다.</li>
 *   <li>지원금 마감 — 아이 나이에 맞는 정책 중 신청 마감이 구간 안에 있는 것.</li>
 *   <li>3월 신학기 — 시설 입소·반 승급이 몰리는 시점. <b>신청 일정은 시설마다 달라 날짜를 만들지 않고</b>
 *       참고(INFO) 항목으로만 둔다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildTimelineService {

    private static final int DEFAULT_MONTHS = 12;
    private static final int MAX_MONTHS = 36;

    /** 어린이집·유치원 입소와 반 승급이 몰리는 달. */
    private static final Month NEW_TERM_MONTH = Month.MARCH;

    /** 신학기 안내를 보여 줄 나이 상한(만). 초등 입학 이후는 이 앱의 범위가 아니다. */
    private static final int NEW_TERM_MAX_AGE_YEARS = 7;

    private final ChildService childService;
    private final VaccinationScheduleRepository vaccinationScheduleRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final PolicyRepository policyRepository;

    public ChildTimelineResponse timeline(Long childId, Integer months) {
        // 소유권 검증은 여기서 한 번만 한다. 남의 아이면 404 (존재 여부를 숨긴다).
        Child child = childService.requireOwned(childId);

        LocalDate today = LocalDate.now();
        LocalDate to = today.plusMonths(normalizeMonths(months));
        LocalDate birthDate = child.getBirthDate();

        List<ChildTimelineResponse.TimelineItem> items = new ArrayList<>();
        items.addAll(vaccinationItems(childId, today, to, birthDate));
        if (birthDate != null) {
            items.addAll(checkupItems(childId, today, to, birthDate));
            items.addAll(newTermItems(today, to, birthDate));
        }
        items.addAll(policyDeadlineItems(child, today, to, birthDate));

        items.sort(Comparator.comparing(ChildTimelineResponse.TimelineItem::getDate));

        return ChildTimelineResponse.builder()
                .childId(childId)
                .childName(child.getName())
                .birthDate(birthDate)
                .from(today)
                .to(to)
                .overdueCount(count(items, "OVERDUE"))
                .upcomingCount(count(items, "UPCOMING"))
                .items(items)
                .build();
    }

    private int normalizeMonths(Integer months) {
        if (months == null || months <= 0) {
            return DEFAULT_MONTHS;
        }
        return Math.min(months, MAX_MONTHS);
    }

    /** 접종. 놓친 것은 구간 시작 전이라도 담는다 — 지난 일이라고 감추면 맞을 기회를 잃는다. */
    private List<ChildTimelineResponse.TimelineItem> vaccinationItems(
            Long childId, LocalDate today, LocalDate to, LocalDate birthDate) {

        List<ChildTimelineResponse.TimelineItem> items = new ArrayList<>();
        for (VaccinationSchedule schedule : vaccinationScheduleRepository.findByChildIdOrderByDueDateAsc(childId)) {
            if (schedule.getStatus() == VaccinationSchedule.VaccinationStatus.COMPLETED
                    || schedule.getStatus() == VaccinationSchedule.VaccinationStatus.SKIPPED) {
                continue;
            }
            LocalDate dueDate = schedule.getDueDate();
            if (dueDate == null || dueDate.isAfter(to)) {
                continue;
            }
            boolean overdue = dueDate.isBefore(today);
            items.add(ChildTimelineResponse.TimelineItem.builder()
                    .date(dueDate)
                    .type("VACCINATION")
                    .status(overdue ? "OVERDUE" : "UPCOMING")
                    .title(schedule.getVaccineType().getDisplayName() + " " + schedule.getDoseNumber() + "차")
                    .description(overdue ? "권장 시기가 지났습니다. 병원에서 접종 가능 여부를 확인하세요." : "권장 접종 시기입니다.")
                    .referenceId(String.valueOf(schedule.getId()))
                    .ageMonths(ageMonths(birthDate, dueDate))
                    .build());
        }
        return items;
    }

    /**
     * 검진. 표준 시기와 이미 남은 검진 기록을 맞춰 본다.
     *
     * <p>기록이 그 구간 안에 있으면 받은 것으로 본다. 검진 기록에 회차가 없으므로 날짜로 판단하는 것이
     * 지금 데이터로 할 수 있는 최선이다 — 회차를 받게 되면 여기만 고치면 된다.
     */
    private List<ChildTimelineResponse.TimelineItem> checkupItems(
            Long childId, LocalDate today, LocalDate to, LocalDate birthDate) {

        List<LocalDate> checkupDates = healthRecordRepository
                .findByChildIdAndRecordType(childId, HealthRecord.RecordType.CHECKUP).stream()
                .map(HealthRecord::getRecordDate)
                .filter(date -> date != null)
                .toList();

        List<ChildTimelineResponse.TimelineItem> items = new ArrayList<>();
        for (CheckupStandard standard : CheckupStandard.all()) {
            LocalDate start = standard.windowStart(birthDate);
            LocalDate end = standard.windowEnd(birthDate);
            if (start.isAfter(to)) {
                continue;
            }

            boolean done = checkupDates.stream()
                    .anyMatch(date -> !date.isBefore(start) && !date.isAfter(end));
            boolean past = end.isBefore(today);
            if (done && past) {
                // 이미 받았고 시기도 지난 항목은 앞으로 할 일이 아니다.
                continue;
            }

            items.add(ChildTimelineResponse.TimelineItem.builder()
                    .date(start.isBefore(today) && !past ? today : start)
                    .type("CHECKUP")
                    .status(done ? "DONE" : past ? "OVERDUE" : "UPCOMING")
                    .title(standard.getTitle())
                    .description("권장 시기 " + standard.getPeriodLabel()
                            + " (" + start + " ~ " + end + ")"
                            + (done ? " · 이 시기에 받은 기록이 있습니다" : ""))
                    .referenceId("checkup-" + standard.getRound())
                    .ageMonths(ageMonths(birthDate, start))
                    .build());
        }
        return items;
    }

    /** 지원금 신청 마감. 아이 나이에 해당하는 정책만. */
    private List<ChildTimelineResponse.TimelineItem> policyDeadlineItems(
            Child child, LocalDate today, LocalDate to, LocalDate birthDate) {

        // Child.getAge() 는 연 나이다. 정책 추천·검색이 같은 값을 쓰므로 여기서 다른 기준을 쓰면
        // 같은 아이가 화면마다 다른 정책을 보게 된다.
        int ageYears = child.getAge();

        List<ChildTimelineResponse.TimelineItem> items = new ArrayList<>();
        for (Policy policy : policyRepository.findDeadlinesForChildAge(today, to, ageYears)) {
            items.add(ChildTimelineResponse.TimelineItem.builder()
                    .date(policy.getApplicationEndDate())
                    .type("POLICY_DEADLINE")
                    .status("UPCOMING")
                    .title(policy.getTitle() + " 신청 마감")
                    .description(policy.getApplicationEndDate() + " 까지 신청해야 합니다."
                            + (policy.getVerifiedAt() == null ? " (금액·조건은 추정치이며 기관 확인이 필요합니다)" : ""))
                    .referenceId(String.valueOf(policy.getId()))
                    .ageMonths(ageMonths(birthDate, policy.getApplicationEndDate()))
                    .build());
        }
        return items;
    }

    /**
     * 3월 신학기. 날짜를 만들어 내지 않는다 — 시설마다 신청 일정이 달라서, 구체적인 날짜를 적으면
     * 그걸 믿고 놓치는 사람이 생긴다. "이 시점을 기억하라" 는 참고 항목으로만 둔다.
     */
    private List<ChildTimelineResponse.TimelineItem> newTermItems(LocalDate today, LocalDate to, LocalDate birthDate) {
        List<ChildTimelineResponse.TimelineItem> items = new ArrayList<>();
        LocalDate term = LocalDate.of(today.getYear(), NEW_TERM_MONTH, 1);
        while (!term.isAfter(to)) {
            if (!term.isBefore(today) && ChronoUnit.YEARS.between(birthDate, term) < NEW_TERM_MAX_AGE_YEARS) {
                items.add(ChildTimelineResponse.TimelineItem.builder()
                        .date(term)
                        .type("NEW_TERM")
                        .status("INFO")
                        .title("3월 신학기")
                        .description("어린이집·유치원 입소와 반 승급이 몰리는 시점입니다. "
                                + "신청 일정은 시설마다 다르므로 관심 시설에 직접 확인하세요.")
                        .ageMonths(ageMonths(birthDate, term))
                        .build());
            }
            term = term.plusYears(1);
        }
        return items;
    }

    private static Integer ageMonths(LocalDate birthDate, LocalDate date) {
        if (birthDate == null || date == null) {
            return null;
        }
        return (int) ChronoUnit.MONTHS.between(birthDate, date);
    }

    private static int count(List<ChildTimelineResponse.TimelineItem> items, String status) {
        return (int) items.stream().filter(item -> status.equals(item.getStatus())).count();
    }
}
