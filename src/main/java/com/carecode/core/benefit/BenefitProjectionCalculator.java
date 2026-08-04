package com.carecode.core.benefit;

import com.carecode.domain.policy.entity.Policy;
import org.springframework.stereotype.Component;

/**
 * 아이의 현재 월령과 전망 기간으로 정책 하나의 예상 수령액을 계산한다.
 * 부풀리면 제품 신뢰가 끝나므로, 판별이 애매하면 항상 적게 잡는다.
 */
@Component
public class BenefitProjectionCalculator {

    /** 정책 한 건의 전망 결과. */
    public record Projection(long amount, int eligibleMonths, BenefitPaymentType paymentType) {

        public boolean isCash() {
            return amount > 0;
        }

        static Projection none(BenefitPaymentType type) {
            return new Projection(0, 0, type);
        }
    }

    /**
     * @param currentAgeMonths 아이의 현재 월령
     * @param horizonMonths    앞으로 몇 개월을 볼 것인지
     */
    public Projection project(Policy policy, int currentAgeMonths, int horizonMonths) {
        BenefitPaymentType type = BenefitPaymentType.resolve(policy.getBenefitType());

        int eligibleMonths = countEligibleMonths(policy, currentAgeMonths, horizonMonths);
        if (eligibleMonths == 0) {
            return Projection.none(type);
        }
        if (type == BenefitPaymentType.NON_CASH) {
            // 혜택은 받지만 금액으로 환산하지 않는다. 건수는 별도로 보여준다.
            return new Projection(0, eligibleMonths, type);
        }

        Integer amount = policy.getBenefitAmount();
        if (amount == null || amount <= 0) {
            return new Projection(0, eligibleMonths, type);
        }

        // UNKNOWN 을 월 지급으로 가정하면 5년 기준 최대 60배 과대 계상된다. 1회로 본다.
        long total = type == BenefitPaymentType.MONTHLY
                ? (long) amount * eligibleMonths
                : amount;

        return new Projection(total, eligibleMonths, type);
    }

    /**
     * 전망 구간 [현재월령, 현재월령+기간) 과 정책 대상 구간 [min, max] 의 겹치는 개월 수.
     * 연령 조건이 없는 정책은 기간 내내 대상으로 본다.
     */
    private int countEligibleMonths(Policy policy, int currentAgeMonths, int horizonMonths) {
        if (horizonMonths <= 0) {
            return 0;
        }
        int windowStart = currentAgeMonths;
        int windowEnd = currentAgeMonths + horizonMonths - 1;

        int policyStart = policy.getTargetAgeMin() != null ? policy.getTargetAgeMin() : 0;
        int policyEnd = policy.getTargetAgeMax() != null ? policy.getTargetAgeMax() : Integer.MAX_VALUE - 1;

        int overlapStart = Math.max(windowStart, policyStart);
        int overlapEnd = Math.min(windowEnd, policyEnd);
        return Math.max(0, overlapEnd - overlapStart + 1);
    }
}
