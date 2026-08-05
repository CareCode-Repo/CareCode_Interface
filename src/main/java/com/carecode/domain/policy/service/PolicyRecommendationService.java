package com.carecode.domain.policy.service;

import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.policy.dto.response.PersonalizedPolicyResponse;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.mapper.PolicyMapper;
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

/** 아이 월령과 거주지로 정책을 추천한다. 협업 필터링은 사용자 로그가 쌓인 뒤에나 의미가 있다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyRecommendationService {

    private static final int CANDIDATE_SIZE = 200;
    private static final int SCORE_AGE_MATCH = 5;
    private static final int SCORE_REGION_MATCH = 3;
    private static final int SCORE_DEADLINE_SOON = 2;
    private static final int DEADLINE_SOON_DAYS = 30;

    private final PolicyRepository policyRepository;
    private final ChildRepository childRepository;
    private final PolicyMapper policyMapper;
    private final CurrentUserFacade currentUserFacade;

    /** 로그인 사용자에게 맞는 정책을 점수 순으로 반환한다. */
    public List<PersonalizedPolicyResponse> recommendForCurrentUser(int limit) {
        User user = currentUserFacade.requireCurrentUser();
        List<Child> children = childRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        LocalDate today = LocalDate.now();

        List<Policy> candidates = policyRepository
                .findByIsActiveTrueOrderByPriorityDescViewCountDesc(PageRequest.of(0, CANDIDATE_SIZE))
                .getContent();

        List<PersonalizedPolicyResponse> scored = new ArrayList<>();
        for (Policy policy : candidates) {
            if (isExpired(policy, today)) {
                continue;
            }
            List<String> reasons = new ArrayList<>();
            int score = score(policy, user, children, today, reasons);
            if (score <= 0) {
                continue;
            }
            scored.add(PersonalizedPolicyResponse.builder()
                    .policy(policyMapper.toResponse(policy))
                    .score(score)
                    .reasons(reasons)
                    .build());
        }

        scored.sort(Comparator.comparingInt(PersonalizedPolicyResponse::getScore).reversed());
        return scored.size() > limit ? scored.subList(0, limit) : scored;
    }

    /** 연령·소득·자녀수 조건에 맞지 않으면 0점으로 제외한다. */
    private int score(Policy policy, User user, List<Child> children, LocalDate today, List<String> reasons) {
        if (!meetsHouseholdConditions(policy, user, children.size(), reasons)) {
            return 0;
        }

        int score = 1; // 조건 없는 범용 정책도 노출되도록 하는 기본 점수

        boolean hasAgeCondition = policy.getTargetAgeMin() != null || policy.getTargetAgeMax() != null;
        if (hasAgeCondition) {
            Child matched = children.stream().filter(c -> matchesAge(policy, c, today)).findFirst().orElse(null);
            if (matched == null) {
                return 0;
            }
            score += SCORE_AGE_MATCH;
            reasons.add(matched.getName() + " 연령 조건에 해당합니다.");
        }

        if (matchesRegion(policy, user, reasons)) {
            score += SCORE_REGION_MATCH;
        }

        if (isDeadlineNear(policy, today)) {
            score += SCORE_DEADLINE_SOON;
            reasons.add("신청 마감이 " + DEADLINE_SOON_DAYS + "일 이내입니다.");
        }

        Integer priority = policy.getPriority();
        if (priority != null && priority > 0) {
            score += Math.min(priority, 3);
        }
        return score;
    }

    /** 소득·자녀수 요건을 확인한다. */
    private boolean meetsHouseholdConditions(Policy policy, User user, int childCount, List<String> reasons) {
        Integer minChildren = policy.getMinChildren();
        if (minChildren != null) {
            if (childCount < minChildren) {
                return false;
            }
            reasons.add("자녀 " + minChildren + "명 이상 대상 정책입니다.");
        }

        Integer threshold = policy.getIncomeThresholdPercent();
        if (threshold == null) {
            return true;
        }
        Integer income = user.getIncomePercent();
        if (income == null) {
            reasons.add("소득 조건이 있는 정책입니다. 소득 정보를 입력하면 정확히 판정됩니다.");
            return true;
        }
        if (income > threshold) {
            return false;
        }
        reasons.add("기준중위소득 " + threshold + "% 이하 대상에 해당합니다.");
        return true;
    }

    /** 정책 대상 월령과 아이의 월령을 비교한다. 시드 데이터 기준 targetAge 단위는 개월이다. */
    private boolean matchesAge(Policy policy, Child child, LocalDate today) {
        if (child.getBirthDate() == null) {
            return false;
        }
        long months = ChronoUnit.MONTHS.between(child.getBirthDate(), today);
        Integer min = policy.getTargetAgeMin();
        Integer max = policy.getTargetAgeMax();
        return (min == null || months >= min) && (max == null || months <= max);
    }

    /** 정책 대상 지역이 사용자 주소와 겹치는지. 전국 정책은 항상 일치로 본다. */
    private boolean matchesRegion(Policy policy, User user, List<String> reasons) {
        String region = policy.getTargetRegion();
        if (region == null || region.isBlank() || region.contains("전국")) {
            reasons.add("전국 어디서나 신청할 수 있습니다.");
            return true;
        }
        String address = user.getAddress();
        if (address == null || address.isBlank()) {
            return false;
        }
        // 주소는 "서울특별시 강남구...", 대상 지역은 "서울" 처럼 표기 단위가 달라 양방향으로 확인한다.
        if (address.contains(region) || region.contains(address)) {
            reasons.add(region + " 거주자 대상 정책입니다.");
            return true;
        }
        return false;
    }

    private boolean isExpired(Policy policy, LocalDate today) {
        LocalDate end = policy.getApplicationEndDate();
        return end != null && end.isBefore(today);
    }

    private boolean isDeadlineNear(Policy policy, LocalDate today) {
        LocalDate end = policy.getApplicationEndDate();
        return end != null && !end.isBefore(today) && ChronoUnit.DAYS.between(today, end) <= DEADLINE_SOON_DAYS;
    }
}
