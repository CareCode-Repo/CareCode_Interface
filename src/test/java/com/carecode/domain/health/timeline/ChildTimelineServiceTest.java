package com.carecode.domain.health.timeline;

import com.carecode.domain.health.dto.response.ChildTimelineResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.entity.VaccinationSchedule;
import com.carecode.domain.health.entity.VaccineType;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.health.repository.VaccinationScheduleRepository;
import com.carecode.domain.health.service.ChildService;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.Child;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 타임라인은 "다음에 뭘 해야 하나" 에 답하는 화면이다. 그래서 놓친 항목을 감추지 않는 것이
 * 가장 중요한 성질이고, 없는 날짜를 만들어 내지 않는 것이 그다음이다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("아이 할 일 타임라인")
class ChildTimelineServiceTest {

    private static final Long CHILD_ID = 7L;

    @Mock ChildService childService;
    @Mock VaccinationScheduleRepository vaccinationScheduleRepository;
    @Mock HealthRecordRepository healthRecordRepository;
    @Mock PolicyRepository policyRepository;

    private ChildTimelineService service;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        service = new ChildTimelineService(childService, vaccinationScheduleRepository,
                healthRecordRepository, policyRepository);
        today = LocalDate.now();
        givenChild(today.minusMonths(13));
        when(vaccinationScheduleRepository.findByChildIdOrderByDueDateAsc(anyLong())).thenReturn(List.of());
        when(healthRecordRepository.findByChildIdAndRecordType(anyLong(), any())).thenReturn(List.of());
        when(policyRepository.findDeadlinesForChildAge(any(), any(), anyInt())).thenReturn(List.of());
    }

    @Test
    @DisplayName("놓친 접종은 구간 시작 전이라도 담고 OVERDUE 로 표시한다")
    void overdueVaccinationIsKept() {
        when(vaccinationScheduleRepository.findByChildIdOrderByDueDateAsc(CHILD_ID))
                .thenReturn(List.of(schedule(1L, VaccineType.HEP_B, 1, today.minusMonths(6),
                        VaccinationSchedule.VaccinationStatus.SCHEDULED)));

        ChildTimelineResponse timeline = service.timeline(CHILD_ID, 12);

        assertThat(items(timeline, "VACCINATION")).hasSize(1);
        assertThat(items(timeline, "VACCINATION").get(0).getStatus()).isEqualTo("OVERDUE");
        // 13개월 아이는 받지 않은 검진(1~3차)도 함께 놓친 항목으로 잡힌다. 합계는 상태별 개수와 같다.
        assertThat(timeline.getOverdueCount())
                .isEqualTo(timeline.getItems().stream().filter(item -> "OVERDUE".equals(item.getStatus())).count())
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("완료·건너뜀 접종은 할 일이 아니라 담지 않는다")
    void completedVaccinationIsExcluded() {
        when(vaccinationScheduleRepository.findByChildIdOrderByDueDateAsc(CHILD_ID))
                .thenReturn(List.of(
                        schedule(1L, VaccineType.HEP_B, 1, today.plusDays(10),
                                VaccinationSchedule.VaccinationStatus.COMPLETED),
                        schedule(2L, VaccineType.HEP_B, 2, today.plusDays(20),
                                VaccinationSchedule.VaccinationStatus.SKIPPED)));

        assertThat(items(service.timeline(CHILD_ID, 12), "VACCINATION")).isEmpty();
    }

    @Test
    @DisplayName("구간 밖의 접종은 담지 않는다")
    void vaccinationBeyondWindowIsExcluded() {
        when(vaccinationScheduleRepository.findByChildIdOrderByDueDateAsc(CHILD_ID))
                .thenReturn(List.of(schedule(1L, VaccineType.HEP_B, 3, today.plusMonths(18),
                        VaccinationSchedule.VaccinationStatus.SCHEDULED)));

        assertThat(items(service.timeline(CHILD_ID, 6), "VACCINATION")).isEmpty();
    }

    @Test
    @DisplayName("표준 검진 시기를 담고, 그 시기에 받은 기록이 있으면 DONE 으로 본다")
    void checkupUsesExistingRecords() {
        // 13개월 아이: 3차(9~12개월)는 시기가 지났고, 4차(18~24개월)는 앞으로다.
        LocalDate thirdRoundVisit = today.minusMonths(2);
        when(healthRecordRepository.findByChildIdAndRecordType(CHILD_ID, HealthRecord.RecordType.CHECKUP))
                .thenReturn(List.of(checkupRecord(thirdRoundVisit)));

        List<ChildTimelineResponse.TimelineItem> checkups = items(service.timeline(CHILD_ID, 24), "CHECKUP");

        assertThat(checkups).isNotEmpty();
        assertThat(checkups).anyMatch(item -> "UPCOMING".equals(item.getStatus()));
        assertThat(checkups).noneMatch(item -> item.getTitle().contains("3차"));
    }

    @Test
    @DisplayName("받지 않고 시기가 지난 검진은 OVERDUE 로 남긴다 — 지났다고 감추면 놓친 걸 모른다")
    void missedCheckupStaysVisible() {
        List<ChildTimelineResponse.TimelineItem> checkups = items(service.timeline(CHILD_ID, 12), "CHECKUP");

        assertThat(checkups).anyMatch(item -> "OVERDUE".equals(item.getStatus()));
    }

    @Test
    @DisplayName("지원금은 아이 나이에 맞는 것만, 마감일에 놓는다")
    void policyDeadlineItems() {
        when(policyRepository.findDeadlinesForChildAge(any(), any(), anyInt()))
                .thenReturn(List.of(policy(11L, "첫만남이용권", today.plusMonths(2))));

        List<ChildTimelineResponse.TimelineItem> policies = items(service.timeline(CHILD_ID, 12), "POLICY_DEADLINE");

        assertThat(policies).hasSize(1);
        assertThat(policies.get(0).getDate()).isEqualTo(today.plusMonths(2));
        assertThat(policies.get(0).getTitle()).contains("첫만남이용권");
        // 검증되지 않은 금액은 추정치라는 사실을 함께 알린다.
        assertThat(policies.get(0).getDescription()).contains("추정치");
    }

    @Test
    @DisplayName("3월 신학기는 참고 항목이고 날짜를 만들어 내지 않는다")
    void newTermIsInfoOnly() {
        List<ChildTimelineResponse.TimelineItem> terms = items(service.timeline(CHILD_ID, 24), "NEW_TERM");

        assertThat(terms).isNotEmpty();
        assertThat(terms).allMatch(item -> "INFO".equals(item.getStatus()));
        assertThat(terms).allMatch(item -> item.getDate().getMonthValue() == 3);
        assertThat(terms.get(0).getDescription()).contains("시설마다 다르므로");
    }

    @Test
    @DisplayName("항목은 날짜순으로 정렬된다")
    void itemsAreSorted() {
        when(vaccinationScheduleRepository.findByChildIdOrderByDueDateAsc(CHILD_ID))
                .thenReturn(List.of(
                        schedule(1L, VaccineType.HEP_B, 2, today.plusMonths(5),
                                VaccinationSchedule.VaccinationStatus.SCHEDULED),
                        schedule(2L, VaccineType.HEP_B, 1, today.minusMonths(1),
                                VaccinationSchedule.VaccinationStatus.SCHEDULED)));
        when(policyRepository.findDeadlinesForChildAge(any(), any(), anyInt()))
                .thenReturn(List.of(policy(11L, "양육수당", today.plusMonths(1))));

        List<LocalDate> dates = service.timeline(CHILD_ID, 12).getItems().stream()
                .map(ChildTimelineResponse.TimelineItem::getDate)
                .toList();

        assertThat(dates).isSorted();
    }

    @Test
    @DisplayName("조회 기간은 상한을 넘지 않는다")
    void windowIsCapped() {
        assertThat(service.timeline(CHILD_ID, 999).getTo()).isEqualTo(today.plusMonths(36));
        assertThat(service.timeline(CHILD_ID, null).getTo()).isEqualTo(today.plusMonths(12));
        assertThat(service.timeline(CHILD_ID, 0).getTo()).isEqualTo(today.plusMonths(12));
    }

    @Test
    @DisplayName("생년월일이 없으면 월령 기반 항목 없이도 동작한다")
    void worksWithoutBirthDate() {
        givenChild(null);

        ChildTimelineResponse timeline = service.timeline(CHILD_ID, 12);

        assertThat(items(timeline, "CHECKUP")).isEmpty();
        assertThat(items(timeline, "NEW_TERM")).isEmpty();
        assertThat(timeline.getBirthDate()).isNull();
    }

    private void givenChild(LocalDate birthDate) {
        when(childService.requireOwned(CHILD_ID)).thenReturn(Child.builder()
                .id(CHILD_ID)
                .name("아이")
                .birthDate(birthDate)
                .build());
    }

    private static List<ChildTimelineResponse.TimelineItem> items(ChildTimelineResponse timeline, String type) {
        return timeline.getItems().stream().filter(item -> type.equals(item.getType())).toList();
    }

    private static VaccinationSchedule schedule(Long id, VaccineType type, int dose, LocalDate dueDate,
                                                VaccinationSchedule.VaccinationStatus status) {
        return VaccinationSchedule.builder()
                .id(id)
                .child(Child.builder().id(CHILD_ID).build())
                .vaccineType(type)
                .doseNumber(dose)
                .dueDate(dueDate)
                .status(status)
                .build();
    }

    private static HealthRecord checkupRecord(LocalDate date) {
        HealthRecord record = new HealthRecord();
        record.setRecordType(HealthRecord.RecordType.CHECKUP);
        record.setRecordDate(date);
        return record;
    }

    private static Policy policy(Long id, String title, LocalDate deadline) {
        return Policy.builder()
                .id(id)
                .title(title)
                .applicationEndDate(deadline)
                .isActive(true)
                .build();
    }
}
