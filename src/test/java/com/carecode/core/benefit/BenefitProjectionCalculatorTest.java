package com.carecode.core.benefit;

import com.carecode.domain.policy.entity.Policy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지원금 전망 계산")
class BenefitProjectionCalculatorTest {

    private final BenefitProjectionCalculator calculator = new BenefitProjectionCalculator();

    @Nested
    @DisplayName("지급 방식 판별")
    class PaymentTypeResolution {

        @Test
        @DisplayName("월 지급 표기가 제각각이어도 인식한다")
        void recognizesMonthlyVariants() {
            assertThat(BenefitPaymentType.resolve("월지급")).isEqualTo(BenefitPaymentType.MONTHLY);
            assertThat(BenefitPaymentType.resolve("월지원")).isEqualTo(BenefitPaymentType.MONTHLY);
            assertThat(BenefitPaymentType.resolve("월급여")).isEqualTo(BenefitPaymentType.MONTHLY);
            assertThat(BenefitPaymentType.resolve("매월 지급")).isEqualTo(BenefitPaymentType.MONTHLY);
        }

        @Test
        @DisplayName("일시 지급을 구분한다")
        void recognizesOneTime() {
            assertThat(BenefitPaymentType.resolve("일시지급")).isEqualTo(BenefitPaymentType.ONE_TIME);
        }

        @Test
        @DisplayName("현금이 아닌 혜택을 걸러낸다")
        void recognizesNonCash() {
            assertThat(BenefitPaymentType.resolve("서비스제공")).isEqualTo(BenefitPaymentType.NON_CASH);
            assertThat(BenefitPaymentType.resolve("무료검진")).isEqualTo(BenefitPaymentType.NON_CASH);
            assertThat(BenefitPaymentType.resolve("이용료할인")).isEqualTo(BenefitPaymentType.NON_CASH);
            assertThat(BenefitPaymentType.resolve("특별공급")).isEqualTo(BenefitPaymentType.NON_CASH);
        }

        @Test
        @DisplayName("표기가 없으면 판별하지 않는다")
        void unknownWhenBlank() {
            assertThat(BenefitPaymentType.resolve(null)).isEqualTo(BenefitPaymentType.UNKNOWN);
            assertThat(BenefitPaymentType.resolve("")).isEqualTo(BenefitPaymentType.UNKNOWN);
        }
    }

    @Test
    @DisplayName("월 지급은 대상 개월 수만큼 곱한다")
    void multipliesMonthlyBenefit() {
        // 12개월 아이, 대상 0~23개월 → 남은 12개월 수령
        Policy policy = policy(0, 23, 350000, "월지급");

        BenefitProjectionCalculator.Projection result = calculator.project(policy, 12, 60);

        assertThat(result.eligibleMonths()).isEqualTo(12);
        assertThat(result.amount()).isEqualTo(12L * 350000);
    }

    @Test
    @DisplayName("전망 기간을 넘는 부분은 세지 않는다")
    void clipsAtHorizon() {
        Policy policy = policy(0, 71, 100000, "월지급");

        BenefitProjectionCalculator.Projection result = calculator.project(policy, 0, 12);

        assertThat(result.eligibleMonths()).isEqualTo(12);
        assertThat(result.amount()).isEqualTo(1_200_000);
    }

    @Test
    @DisplayName("일시금은 대상 기간이 길어도 1회만 계산한다")
    void countsOneTimeOnce() {
        Policy policy = policy(0, 23, 2000000, "일시지급");

        BenefitProjectionCalculator.Projection result = calculator.project(policy, 0, 24);

        assertThat(result.amount()).isEqualTo(2_000_000);
    }

    @Test
    @DisplayName("지급 방식이 불명확하면 1회로 본다 — 과대 계상을 피한다")
    void treatsUnknownAsOneTime() {
        Policy policy = policy(0, 59, 500000, null);

        BenefitProjectionCalculator.Projection result = calculator.project(policy, 0, 60);

        // 월 지급으로 오인하면 3천만원이 된다
        assertThat(result.amount()).isEqualTo(500_000);
    }

    @Test
    @DisplayName("현금이 아닌 혜택은 금액에 넣지 않는다")
    void excludesNonCashFromAmount() {
        Policy policy = policy(0, 59, 300000, "무료검진");

        BenefitProjectionCalculator.Projection result = calculator.project(policy, 0, 60);

        assertThat(result.amount()).isZero();
        assertThat(result.eligibleMonths()).isPositive();
        assertThat(result.paymentType()).isEqualTo(BenefitPaymentType.NON_CASH);
    }

    @Test
    @DisplayName("대상 연령이 이미 지났으면 0원이다")
    void zeroWhenAgeWindowPassed() {
        Policy policy = policy(0, 23, 350000, "월지급");

        BenefitProjectionCalculator.Projection result = calculator.project(policy, 30, 60);

        assertThat(result.eligibleMonths()).isZero();
        assertThat(result.amount()).isZero();
    }

    @Test
    @DisplayName("아직 대상 연령 전이면 도달 이후분만 센다")
    void countsOnlyFutureEligibleMonths() {
        // 현재 0개월, 대상 36~71개월, 전망 48개월 → 36~47개월 구간 12개월만 해당
        Policy policy = policy(36, 71, 280000, "월지원");

        BenefitProjectionCalculator.Projection result = calculator.project(policy, 0, 48);

        assertThat(result.eligibleMonths()).isEqualTo(12);
    }

    @Test
    @DisplayName("연령 조건이 없으면 기간 내내 대상으로 본다")
    void treatsNoAgeConditionAsAlwaysEligible() {
        Policy policy = policy(null, null, 50000, "월지급");

        BenefitProjectionCalculator.Projection result = calculator.project(policy, 10, 24);

        assertThat(result.eligibleMonths()).isEqualTo(24);
    }

    @Test
    @DisplayName("금액이 없으면 합산하지 않는다")
    void zeroWhenAmountMissing() {
        Policy policy = policy(0, 59, null, "월지급");

        assertThat(calculator.project(policy, 0, 12).amount()).isZero();
    }

    @Test
    @DisplayName("전망 기간이 0이면 계산하지 않는다")
    void zeroWhenHorizonEmpty() {
        Policy policy = policy(0, 59, 100000, "월지급");

        assertThat(calculator.project(policy, 0, 0).eligibleMonths()).isZero();
    }

    @Test
    @DisplayName("지급 개월 상한이 있으면 대상 기간이 길어도 그만큼만 준다")
    void capsByPaymentDuration() {
        // 육아휴직급여: 대상 0~96개월이지만 실제 지급은 최대 12개월
        Policy policy = policy(0, 96, 1_500_000, "월급여");
        policy.setMaxPaymentMonths(12);

        BenefitProjectionCalculator.Projection result = calculator.project(policy, 0, 60);

        // 상한이 없으면 9,000만원이 된다
        assertThat(result.eligibleMonths()).isEqualTo(12);
        assertThat(result.amount()).isEqualTo(18_000_000);
    }

    @Test
    @DisplayName("대상 기간이 상한보다 짧으면 짧은 쪽을 따른다")
    void usesShorterOfWindowAndCap() {
        Policy policy = policy(0, 5, 1_000_000, "월지급");
        policy.setMaxPaymentMonths(12);

        assertThat(calculator.project(policy, 0, 60).eligibleMonths()).isEqualTo(6);
    }

    @Test
    @DisplayName("일시금에는 지급 개월 상한이 영향을 주지 않는다")
    void capDoesNotAffectOneTime() {
        Policy policy = policy(0, 96, 2_000_000, "일시지급");
        policy.setMaxPaymentMonths(3);

        assertThat(calculator.project(policy, 0, 60).amount()).isEqualTo(2_000_000);
    }

    private Policy policy(Integer ageMin, Integer ageMax, Integer amount, String benefitType) {
        Policy p = new Policy();
        p.setTargetAgeMin(ageMin);
        p.setTargetAgeMax(ageMax);
        p.setBenefitAmount(amount);
        p.setBenefitType(benefitType);
        return p;
    }
}
