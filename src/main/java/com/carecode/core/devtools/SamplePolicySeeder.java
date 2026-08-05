package com.carecode.core.devtools;

import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 거주지별 지원금 비교를 확인하기 위한 샘플 정책. 실제 지자체 금액이 아니라 기능 확인용 임의값이다 — 공공데이터 연동 전까지만 쓴다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SamplePolicySeeder {

    private final PolicyRepository policyRepository;

    @Transactional
    public int seed() {
        List<Policy> policies = buildPolicies();
        int created = 0;

        for (Policy policy : policies) {
            if (policyRepository.findByPolicyCode(policy.getPolicyCode()).isPresent()) {
                continue; // 이미 넣었으면 건너뛴다 (재기동 시 중복 방지)
            }
            policyRepository.save(policy);
            created++;
        }
        return created;
    }

    private List<Policy> buildPolicies() {
        return List.of(
                // ── 전국 공통: 어디 살든 받는다. 지역 비교의 공통 기준선이 된다.
                policy("NATION-01", "부모급여(0세)", "전국", 0, 11, 1_000_000, "월지급", 12),
                policy("NATION-02", "부모급여(1세)", "전국", 12, 23, 500_000, "월지급", 12),
                policy("NATION-03", "아동수당", "전국", 0, 95, 100_000, "월지급", 6),
                policy("NATION-04", "첫만남이용권", "전국", 0, 11, 2_000_000, "일시지급", 12),
                policy("NATION-05", "영유아 건강검진", "전국", 0, 71, 0, "무료검진", null),

                // ── 수도권: 전국 정책 외 추가 지원이 적다.
                policy("REGION-01", "출산축하금", "성남시", 0, 11, 300_000, "일시지급", 6),
                policy("REGION-02", "산후조리비 지원", "서울특별시", 0, 5, 1_000_000, "일시지급", 6),

                // ── 인구감소지역: 전입·출산 지원이 크다. 차액이 드러나는 지점.
                policy("REGION-03", "출산장려금(1인당)", "고흥군", 0, 23, 7_200_000, "일시지급", 12),
                policy("REGION-04", "양육비 추가지원", "고흥군", 0, 59, 200_000, "월지급", 6),
                policy("REGION-05", "전입가구 정착지원금", "고흥군", 0, 95, 3_000_000, "일시지급", null),
                policy("REGION-06", "출산장려금", "의성군", 0, 23, 5_000_000, "일시지급", 12),
                policy("REGION-07", "육아용품 구입비", "의성군", 0, 35, 150_000, "월지원", 6),
                policy("REGION-08", "다자녀 양육지원금", "해남군", 0, 71, 300_000, "월지급", 12),

                // ── 다자녀 요건: 자녀 수 조건 동작 확인용
                multiChildPolicy("REGION-09", "셋째아 이상 지원금", "고흥군", 0, 59, 500_000, 3),

                // ── 소득 요건: 소득구간 판정 동작 확인용
                incomeCappedPolicy("REGION-10", "저소득 양육지원", "성남시", 0, 59, 400_000, 150)
        );
    }

    private Policy policy(String code, String title, String region, int ageMin, int ageMax,
                          int amount, String benefitType, Integer retroactiveMonths) {
        return base(code, title, region, ageMin, ageMax, amount, benefitType, retroactiveMonths).build();
    }

    private Policy multiChildPolicy(String code, String title, String region, int ageMin, int ageMax,
                                    int amount, int minChildren) {
        return base(code, title, region, ageMin, ageMax, amount, "월지급", 12)
                .minChildren(minChildren)
                .build();
    }

    private Policy incomeCappedPolicy(String code, String title, String region, int ageMin, int ageMax,
                                      int amount, int incomeThreshold) {
        return base(code, title, region, ageMin, ageMax, amount, "월지급", 12)
                .incomeThresholdPercent(incomeThreshold)
                .build();
    }

    private Policy.PolicyBuilder base(String code, String title, String region, int ageMin, int ageMax,
                                      int amount, String benefitType, Integer retroactiveMonths) {
        return Policy.builder()
                .policyCode(SampleDataProperties.POLICY_PREFIX + code)
                .title(title)
                .description("[샘플] 기능 확인용 데이터입니다. 실제 지원 금액이 아닙니다.")
                .policyType("현금지원")
                .targetRegion(region)
                .targetAgeMin(ageMin)
                .targetAgeMax(ageMax)
                .benefitAmount(amount)
                .benefitType(benefitType)
                .retroactiveMonths(retroactiveMonths)
                .isActive(true)
                .priority(1)
                .viewCount(0);
    }
}
