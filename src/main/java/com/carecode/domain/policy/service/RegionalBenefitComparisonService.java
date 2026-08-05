package com.carecode.domain.policy.service;

import com.carecode.core.benefit.BenefitPaymentType;
import com.carecode.core.benefit.BenefitProjectionCalculator;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.policy.dto.response.RegionalBenefitComparisonResponse;
import com.carecode.domain.policy.dto.response.RegionalBenefitResponse;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 같은 아이라도 사는 지역에 따라 받는 지원금 총액이 크게 다르다. 지역별 예상 수령액을 계산해 현재 거주지와 비교한다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionalBenefitComparisonService {

    private static final int DEFAULT_HORIZON_MONTHS = 60;
    private static final int MAX_HORIZON_MONTHS = 240;
    private static final int DEFAULT_LIMIT = 10;
    private static final int TOP_CONTRIBUTORS = 3;

    private final PolicyRepository policyRepository;
    private final ChildRepository childRepository;
    private final CurrentUserFacade currentUserFacade;
    private final EventLogger eventLogger;
    private final BenefitProjectionCalculator calculator;

    public RegionalBenefitComparisonResponse compare(Long childId, Integer years, Integer limit) {
        User user = currentUserFacade.requireCurrentUser();
        List<Child> children = childRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        Child child = resolveChild(children, childId);

        int horizon = resolveHorizon(years);
        int currentAgeMonths = (int) ChronoUnit.MONTHS.between(child.getBirthDate(), LocalDate.now());

        // 자격이 안 되는 정책을 총액에 넣으면 "이사하면 얼마 더" 가 통째로 틀어진다.
        List<Policy> activePolicies = policyRepository.findByIsActiveTrue().stream()
                .filter(p -> meetsHouseholdConditions(p, user, children.size()))
                .toList();
        long conditional = activePolicies.stream().filter(p -> isIncomeConditional(p, user)).count();
        List<Policy> nationwide = activePolicies.stream().filter(this::isNationwide).toList();
        List<String> regions = policyRepository.findDistinctTargetRegions();

        // 전국 정책은 어디에 살든 받으므로 모든 지역에 공통으로 얹는다.
        RegionSummary nationwideBase = summarize(nationwide, currentAgeMonths, horizon);

        Map<String, RegionSummary> summaries = new LinkedHashMap<>();
        for (String region : regions) {
            List<Policy> regional = activePolicies.stream()
                    .filter(p -> region.equals(p.getTargetRegion()))
                    .toList();
            summaries.put(region, summarize(regional, currentAgeMonths, horizon).merge(nationwideBase));
        }

        // 차액을 내려면 기준액이 먼저 정해져야 하므로 응답은 그 뒤에 만든다.
        String baseRegion = findBaseRegion(user, regions);
        RegionSummary baseSummary = baseRegion == null ? null : summaries.get(baseRegion);
        // 기준 지역이 없으면 전국 공통 정책만 받는 것으로 보고 비교한다.
        long base = baseSummary != null ? baseSummary.amount() : nationwideBase.amount();

        List<RegionalBenefitResponse> rankings = summaries.entrySet().stream()
                .map(e -> e.getValue().toResponse(e.getKey(), base))
                .sorted(Comparator.comparingLong(RegionalBenefitResponse::getTotalAmount).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        int size = limit != null && limit > 0 ? limit : DEFAULT_LIMIT;

        eventLogger.log(EventType.REGIONAL_COMPARISON_VIEWED, user.getId(), baseRegion);

        return RegionalBenefitComparisonResponse.builder()
                .childName(child.getName())
                .childAgeMonths(currentAgeMonths)
                .horizonMonths(horizon)
                .baseRegion(baseRegion)
                .baseAmount(base)
                .rankings(rankings.size() > size ? rankings.subList(0, size) : rankings)
                .dataQuality("ESTIMATED")
                .disclaimers(buildDisclaimers(baseRegion, conditional))
                .build();
    }

    /** 자녀 수는 확정값이라 못 맞추면 제외한다. 소득은 미입력을 탈락으로 보지 않는다. */
    private boolean meetsHouseholdConditions(Policy policy, User user, int childCount) {
        Integer minChildren = policy.getMinChildren();
        if (minChildren != null && childCount < minChildren) {
            return false;
        }
        Integer threshold = policy.getIncomeThresholdPercent();
        Integer income = user.getIncomePercent();
        return threshold == null || income == null || income <= threshold;
    }

    /** 소득 조건이 있는데 사용자가 소득을 입력하지 않아 판정을 보류한 정책. */
    private boolean isIncomeConditional(Policy policy, User user) {
        return policy.getIncomeThresholdPercent() != null && user.getIncomePercent() == null;
    }

    /** 지역 한 곳의 집계 중간 결과. */
    private record RegionSummary(long amount, int cashCount, int nonCashCount, int verifiedCount,
                                 List<RegionalBenefitResponse.Contribution> contributions) {

        RegionSummary merge(RegionSummary other) {
            List<RegionalBenefitResponse.Contribution> merged = new ArrayList<>(contributions);
            merged.addAll(other.contributions);
            return new RegionSummary(amount + other.amount, cashCount + other.cashCount,
                    nonCashCount + other.nonCashCount, verifiedCount + other.verifiedCount, merged);
        }

        /** 금액에 들어간 정책이 전부 검증됐을 때만 확정으로 표기한다. */
        String quality() {
            if (cashCount == 0) {
                return "ESTIMATED";
            }
            if (verifiedCount == cashCount) {
                return "VERIFIED";
            }
            return verifiedCount > 0 ? "PARTIAL" : "ESTIMATED";
        }

        RegionalBenefitResponse toResponse(String region, long baseAmount) {
            List<RegionalBenefitResponse.Contribution> top = contributions.stream()
                    .sorted(Comparator.comparingLong(RegionalBenefitResponse.Contribution::getAmount).reversed())
                    .limit(TOP_CONTRIBUTORS)
                    .toList();

            return RegionalBenefitResponse.builder()
                    .region(region)
                    .totalAmount(amount)
                    .differenceFromBase(amount - baseAmount)
                    .cashPolicyCount(cashCount)
                    .nonCashPolicyCount(nonCashCount)
                    .verifiedPolicyCount(verifiedCount)
                    .dataQuality(quality())
                    .topContributors(top)
                    .build();
        }
    }

    private RegionSummary summarize(List<Policy> policies, int ageMonths, int horizon) {
        long total = 0;
        int cash = 0;
        int nonCash = 0;
        int verified = 0;
        List<RegionalBenefitResponse.Contribution> contributions = new ArrayList<>();

        for (Policy policy : policies) {
            BenefitProjectionCalculator.Projection projection = calculator.project(policy, ageMonths, horizon);
            if (projection.eligibleMonths() == 0) {
                continue;
            }
            if (projection.paymentType() == BenefitPaymentType.NON_CASH) {
                nonCash++;
                continue;
            }
            if (!projection.isCash()) {
                continue; // 금액이 확인되지 않은 정책은 합산하지 않는다
            }
            total += projection.amount();
            cash++;
            if (policy.getVerifiedAt() != null) {
                verified++;
            }
            contributions.add(RegionalBenefitResponse.Contribution.builder()
                    .title(policy.getTitle())
                    .amount(projection.amount())
                    .paymentType(projection.paymentType().name())
                    .build());
        }
        return new RegionSummary(total, cash, nonCash, verified, contributions);
    }

    private Child resolveChild(List<Child> children, Long childId) {
        if (children.isEmpty()) {
            throw new CareServiceException("등록된 자녀가 없습니다. 자녀를 먼저 등록해 주세요.");
        }
        Child child = childId == null ? children.get(0)
                : children.stream().filter(c -> c.getId().equals(childId)).findFirst()
                .orElseThrow(() -> new CareServiceException("자녀를 찾을 수 없습니다: " + childId));

        if (child.getBirthDate() == null) {
            throw new CareServiceException("자녀의 생년월일이 없어 지원금을 계산할 수 없습니다.");
        }
        return child;
    }

    private int resolveHorizon(Integer years) {
        if (years == null || years <= 0) {
            return DEFAULT_HORIZON_MONTHS;
        }
        return Math.min(years * 12, MAX_HORIZON_MONTHS);
    }

    private boolean isNationwide(Policy policy) {
        String region = policy.getTargetRegion();
        return region == null || region.isBlank() || region.contains("전국");
    }

    /** 사용자 주소에서 정책 지역명을 찾는다. 표기 단위가 달라 양방향으로 확인한다. */
    private String findBaseRegion(User user, List<String> regions) {
        String address = user.getAddress();
        if (address == null || address.isBlank()) {
            return null;
        }
        return regions.stream()
                .filter(r -> address.contains(r) || r.contains(address))
                // 가장 구체적인 지역명을 고른다 ("경기도" 보다 "성남시")
                .max(Comparator.comparingInt(String::length))
                .orElse(null);
    }

    private List<String> buildDisclaimers(String baseRegion, long conditionalCount) {
        List<String> notes = new ArrayList<>();
        notes.add("수집된 정책 기준 추정치이며 실제 수령액과 다를 수 있습니다.");
        // 중복 수급이 불가능한 정책들이 함께 더해질 수 있어, 총액보다 지역 간 차액이 신뢰도가 높다.
        notes.add("총액은 모든 정책을 단순 합산한 값입니다. 상호 배타적인 정책이 포함될 수 있으므로 "
                + "지역 간 '차액' 을 기준으로 보세요.");
        if (conditionalCount > 0) {
            notes.add(String.format("소득 조건이 걸린 정책 %d건은 소득 미입력 상태로 포함했습니다. "
                    + "소득을 입력하면 정확해집니다.", conditionalCount));
        }
        notes.add("무료검진·서비스 등 금액으로 환산할 수 없는 혜택은 합산에서 제외했습니다.");
        notes.add("지급 방식이 명시되지 않은 정책은 과대 계상을 피하기 위해 1회 지급으로 계산했습니다.");
        if (baseRegion == null) {
            notes.add("주소가 등록되지 않아 전국 공통 정책만을 기준으로 비교했습니다.");
        }
        notes.add("전입 지원금은 거주 요건·기간 조건이 붙는 경우가 많으므로 신청 전 해당 지자체에 확인하세요.");
        return notes;
    }
}
