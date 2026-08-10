package com.carecode.domain.policy.service;

import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyChange;
import com.carecode.domain.policy.repository.PolicyChangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 동기화 때 덮어쓰기 전후를 비교해 변경을 기록한다.
 * 알릴 가치가 있는 필드만 본다 — 설명 오탈자까지 알리면 알림이 소음이 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyChangeDetector {

    private final PolicyChangeRepository changeRepository;

    /** 변경 전 상태 스냅샷. 엔티티는 곧 덮어써지므로 값만 복사해 둔다. */
    public record Before(Integer benefitAmount, LocalDate applicationEndDate,
                         Integer targetAgeMin, Integer targetAgeMax) {

        public static Before of(Policy policy) {
            return new Before(policy.getBenefitAmount(), policy.getApplicationEndDate(),
                    policy.getTargetAgeMin(), policy.getTargetAgeMax());
        }
    }

    /** 신규 정책. 해당 지역 사용자에게 알릴 가치가 가장 크다. */
    public void recordCreated(Policy policy) {
        save(PolicyChange.builder()
                .policyId(policy.getId())
                .changeType(PolicyChange.ChangeType.CREATED)
                .newValue(policy.getTitle())
                .targetRegion(policy.getTargetRegion())
                .build());
    }

    public void recordUpdates(Policy policy, Before before) {
        List<PolicyChange> changes = new ArrayList<>();

        if (!Objects.equals(before.benefitAmount(), policy.getBenefitAmount())
                && policy.getBenefitAmount() != null) {
            changes.add(build(policy, PolicyChange.ChangeType.AMOUNT_CHANGED, "benefitAmount",
                    text(before.benefitAmount()), text(policy.getBenefitAmount())));
        }
        if (!Objects.equals(before.applicationEndDate(), policy.getApplicationEndDate())) {
            changes.add(build(policy, PolicyChange.ChangeType.DEADLINE_CHANGED, "applicationEndDate",
                    text(before.applicationEndDate()), text(policy.getApplicationEndDate())));
        }
        if (!Objects.equals(before.targetAgeMin(), policy.getTargetAgeMin())
                || !Objects.equals(before.targetAgeMax(), policy.getTargetAgeMax())) {
            changes.add(build(policy, PolicyChange.ChangeType.AGE_RANGE_CHANGED, "targetAge",
                    range(before.targetAgeMin(), before.targetAgeMax()),
                    range(policy.getTargetAgeMin(), policy.getTargetAgeMax())));
        }

        changes.forEach(this::save);
    }

    private PolicyChange build(Policy policy, PolicyChange.ChangeType type,
                               String field, String oldValue, String newValue) {
        return PolicyChange.builder()
                .policyId(policy.getId())
                .changeType(type)
                .fieldName(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .targetRegion(policy.getTargetRegion())
                .build();
    }

    /** 변경 기록 실패가 동기화를 멈추면 안 된다. */
    private void save(PolicyChange change) {
        try {
            changeRepository.save(change);
        } catch (Exception e) {
            log.warn("정책 변경 기록 실패 - policyId={}, 사유={}", change.getPolicyId(), e.getMessage());
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String range(Integer min, Integer max) {
        return (min == null ? "-" : min) + "~" + (max == null ? "-" : max) + "개월";
    }
}
