package com.carecode.domain.policy.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.policy.dto.response.MissedBenefitResponse;
import com.carecode.domain.policy.dto.response.MissedBenefitSummaryResponse;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 아이가 이미 지나온 월령 구간을 훑어 받을 수 있었던 지원금을 찾는다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissedBenefitService {

    private static final int CANDIDATE_SIZE = 300;

    private final PolicyRepository policyRepository;
    private final ChildRepository childRepository;
    private final CurrentUserFacade currentUserFacade;
    private final EventLogger eventLogger;

    public MissedBenefitSummaryResponse findMissedBenefits() {
        User user = currentUserFacade.requireCurrentUser();
        List<Child> children = childRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        LocalDate today = LocalDate.now();

        List<MissedBenefitResponse> claimable = new ArrayList<>();
        List<MissedBenefitResponse> expired = new ArrayList<>();
        int unknownEligibility = 0;

        if (children.isEmpty()) {
            return summarize(claimable, expired, 0);
        }

        List<Policy> candidates = policyRepository
                .findByIsActiveTrueOrderByPriorityDescViewCountDesc(PageRequest.of(0, CANDIDATE_SIZE))
                .getContent();

        for (Child child : children) {
            if (child.getBirthDate() == null) {
                continue;
            }
            int currentMonths = (int) ChronoUnit.MONTHS.between(child.getBirthDate(), today);

            for (Policy policy : candidates) {
                if (!hasPassedAgeWindow(policy, currentMonths)) {
                    continue;
                }
                Eligibility eligibility = checkEligibility(policy, user, children.size());
                if (eligibility == Eligibility.NOT_ELIGIBLE) {
                    continue;
                }
                if (eligibility == Eligibility.UNKNOWN) {
                    unknownEligibility++;
                }

                MissedBenefitResponse item = toResponse(policy, child, currentMonths, eligibility, today);
                if (item.isClaimable()) {
                    claimable.add(item);
                } else {
                    expired.add(item);
                }
            }
        }

        eventLogger.log(EventType.MISSED_BENEFIT_VIEWED, user.getId(),
                null, "claimable=" + claimable.size());

        claimable.sort(Comparator.comparingInt(
                (MissedBenefitResponse m) -> m.getBenefitAmount() == null ? 0 : m.getBenefitAmount()).reversed());
        return summarize(claimable, expired, unknownEligibility);
    }

    /** 아이가 대상 연령 구간을 이미 지났는지. 아직 대상이면 "놓친" 것이 아니다. */
    private boolean hasPassedAgeWindow(Policy policy, int currentMonths) {
        Integer max = policy.getTargetAgeMax();
        if (max == null) {
            return false; // 상한이 없으면 지금도 대상이다
        }
        Integer min = policy.getTargetAgeMin();
        if (min != null && currentMonths < min) {
            return false; // 아직 대상 연령에 도달하지 않았다
        }
        return currentMonths > max;
    }

    private enum Eligibility {
        ELIGIBLE, NOT_ELIGIBLE, UNKNOWN
    }

    /** 소득·다자녀 조건을 본다. 사용자가 소득을 입력하지 않았으면 배제하지 않고 보류한다. */
    private Eligibility checkEligibility(Policy policy, User user, int childCount) {
        Integer minChildren = policy.getMinChildren();
        if (minChildren != null && childCount < minChildren) {
            return Eligibility.NOT_ELIGIBLE;
        }

        Integer threshold = policy.getIncomeThresholdPercent();
        if (threshold == null) {
            return Eligibility.ELIGIBLE;
        }
        Integer income = user.getIncomePercent();
        if (income == null) {
            // 소득 미입력을 탈락으로 처리하면 받을 수 있었던 지원금이 통째로 사라진다.
            return Eligibility.UNKNOWN;
        }
        return income <= threshold ? Eligibility.ELIGIBLE : Eligibility.NOT_ELIGIBLE;
    }

    private MissedBenefitResponse toResponse(Policy policy, Child child, int currentMonths,
                                             Eligibility eligibility, LocalDate today) {
        List<String> reasons = new ArrayList<>();
        reasons.add(String.format("%s 님이 %d~%d개월이던 시기에 대상이었습니다.",
                child.getName(),
                policy.getTargetAgeMin() != null ? policy.getTargetAgeMin() : 0,
                policy.getTargetAgeMax()));

        // 소급 기간은 대상 연령 상한을 지난 시점부터 센다.
        Integer retroactive = policy.getRetroactiveMonths();
        int monthsSinceIneligible = currentMonths - policy.getTargetAgeMax();
        boolean withinRetroactive = retroactive != null && monthsSinceIneligible <= retroactive;

        // 정책 자체의 신청 마감도 지나지 않아야 한다.
        boolean applicationOpen = policy.getApplicationEndDate() == null
                || !policy.getApplicationEndDate().isBefore(today);

        boolean claimable = withinRetroactive && applicationOpen;
        Integer remaining = claimable ? retroactive - monthsSinceIneligible : null;

        if (claimable) {
            reasons.add(String.format("소급 신청이 %d개월 남았습니다.", remaining));
        } else if (retroactive == null) {
            reasons.add("이 정책은 소급 신청을 받지 않습니다.");
        } else if (!applicationOpen) {
            reasons.add("정책 신청 기간이 종료되었습니다.");
        } else {
            reasons.add(String.format("소급 가능 기간(%d개월)이 지났습니다.", retroactive));
        }

        if (eligibility == Eligibility.UNKNOWN) {
            reasons.add("소득 정보를 입력하면 대상 여부를 정확히 판정할 수 있습니다.");
        }

        return MissedBenefitResponse.builder()
                .policyId(policy.getId())
                .title(policy.getTitle())
                .childName(child.getName())
                .eligibleFromMonth(policy.getTargetAgeMin())
                .eligibleToMonth(policy.getTargetAgeMax())
                .claimable(claimable)
                .remainingMonths(remaining)
                .benefitAmount(policy.getBenefitAmount())
                .applicationUrl(policy.getApplicationUrl())
                .reasons(reasons)
                .build();
    }

    private MissedBenefitSummaryResponse summarize(List<MissedBenefitResponse> claimable,
                                                   List<MissedBenefitResponse> expired,
                                                   int unknownEligibility) {
        long total = claimable.stream()
                .filter(m -> m.getBenefitAmount() != null)
                .mapToLong(MissedBenefitResponse::getBenefitAmount)
                .sum();

        return MissedBenefitSummaryResponse.builder()
                .claimableCount(claimable.size())
                .claimableAmount(total)
                .expiredCount(expired.size())
                .unknownEligibilityCount(unknownEligibility)
                .claimable(claimable)
                .expired(expired)
                .build();
    }
}
