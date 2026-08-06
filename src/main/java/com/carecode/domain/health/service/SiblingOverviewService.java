package com.carecode.domain.health.service;

import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.careFacility.repository.FacilityWaitlistRepository;
import com.carecode.domain.health.dto.response.SiblingOverviewResponse;
import com.carecode.domain.health.entity.VaccinationSchedule;
import com.carecode.domain.health.repository.VaccinationScheduleRepository;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
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
import java.util.List;

/**
 * 자녀 전체를 한 번에 본다.
 * 다자녀 가구는 이 앱이 가장 필요한 집단인데, 화면이 아이 한 명 기준이라 매번 전환해야 했다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiblingOverviewService {

    /** 다자녀 기준. 대부분의 지자체가 2명부터 다자녀로 본다. */
    private static final int MULTI_CHILD_THRESHOLD = 2;

    private final ChildRepository childRepository;
    private final VaccinationScheduleRepository vaccinationRepository;
    private final FacilityWaitlistRepository waitlistRepository;
    private final PolicyRepository policyRepository;
    private final CurrentUserFacade currentUserFacade;

    public SiblingOverviewResponse getOverview() {
        User user = currentUserFacade.requireCurrentUser();
        List<Child> children = childRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<SiblingOverviewResponse.ChildSummary> summaries = children.stream()
                .map(this::toSummary)
                .toList();

        boolean multiChild = children.size() >= MULTI_CHILD_THRESHOLD;

        return SiblingOverviewResponse.builder()
                .childCount(children.size())
                .multiChildHousehold(multiChild)
                .children(summaries)
                .multiChildBenefits(multiChild ? findMultiChildPolicies(children.size(), user) : List.of())
                .notes(buildNotes(children.size(), multiChild))
                .build();
    }

    private SiblingOverviewResponse.ChildSummary toSummary(Child child) {
        Integer months = child.getBirthDate() == null ? null
                : (int) ChronoUnit.MONTHS.between(child.getBirthDate(), LocalDate.now());

        VaccinationSchedule next = findNextVaccination(child);

        return SiblingOverviewResponse.ChildSummary.builder()
                .childId(child.getId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .ageMonths(months)
                .classLabel(classLabel(months))
                .nextVaccination(next == null ? null : next.getVaccineType().name())
                .nextVaccinationDate(next == null ? null : next.getDueDate())
                .waitlistCount(countWaitlists(child))
                .build();
    }

    /** 아직 맞지 않은 것 중 가장 이른 일정. 아이별로 흩어져 있으면 놓치기 쉽다. */
    private VaccinationSchedule findNextVaccination(Child child) {
        LocalDate today = LocalDate.now();
        return vaccinationRepository.findByChildIdOrderByDueDateAsc(child.getId()).stream()
                .filter(v -> v.getCompletedDate() == null)
                .filter(v -> v.getDueDate() != null && !v.getDueDate().isBefore(today))
                .findFirst()
                .orElse(null);
    }

    private long countWaitlists(Child child) {
        try {
            return waitlistRepository.findByUserIdOrderByAppliedAtDesc(child.getUser().getId()).stream()
                    .filter(w -> w.getChild() != null && w.getChild().getId().equals(child.getId()))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    /** 자녀 수 조건이 붙은 정책 중 지금 충족하는 것. */
    private List<String> findMultiChildPolicies(int childCount, User user) {
        return policyRepository.findByIsActiveTrue().stream()
                .filter(p -> p.getMinChildren() != null && p.getMinChildren() <= childCount)
                .filter(p -> matchesRegion(p, user))
                .map(Policy::getTitle)
                .distinct()
                .limit(10)
                .toList();
    }

    private boolean matchesRegion(Policy policy, User user) {
        String region = policy.getTargetRegion();
        if (region == null || region.isBlank() || region.contains("전국")) {
            return true;
        }
        String address = user.getAddress();
        return address != null && (address.contains(region) || region.contains(address));
    }

    /** 어린이집·유치원 반 편성은 만 나이 기준이다. */
    private String classLabel(Integer months) {
        if (months == null) {
            return null;
        }
        int years = months / 12;
        return years >= 5 ? "5세반 이상" : years + "세반";
    }

    private List<String> buildNotes(int childCount, boolean multiChild) {
        List<String> notes = new ArrayList<>();
        if (childCount == 0) {
            notes.add("자녀를 등록하면 맞춤 지원금과 접종 일정을 함께 볼 수 있습니다.");
            return notes;
        }
        if (multiChild) {
            notes.add("다자녀 가구는 어린이집 입소 시 우선순위 가점을 받습니다. 신청 시 확인해 보세요.");
            notes.add("첫째가 다니는 시설에 둘째를 넣으면 형제자매 가점이 추가로 붙는 경우가 많습니다.");
        } else {
            notes.add("자녀가 2명 이상이면 다자녀 지원금과 입소 가점 대상이 됩니다.");
        }
        return notes;
    }
}
