package com.carecode.domain.policy.service;

import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.policy.dto.response.MissedBenefitSummaryResponse;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("놓친 지원금 발굴")
class MissedBenefitServiceTest {

    private PolicyRepository policyRepository;
    private ChildRepository childRepository;
    private CurrentUserFacade currentUserFacade;
    private MissedBenefitService service;

    private User user;

    @BeforeEach
    void setUp() {
        policyRepository = mock(PolicyRepository.class);
        childRepository = mock(ChildRepository.class);
        currentUserFacade = mock(CurrentUserFacade.class);

        user = User.builder().id(1L).name("부모").build();
        when(currentUserFacade.requireCurrentUser()).thenReturn(user);

        service = new MissedBenefitService(policyRepository, childRepository, currentUserFacade);
    }

    @Test
    @DisplayName("아직 대상 연령이면 놓친 것이 아니다")
    void ignoresPolicyStillEligible() {
        givenChildAgedMonths(12);
        givenPolicies(policy("부모급여", 0, 23, 350000, 12));

        MissedBenefitSummaryResponse result = service.findMissedBenefits();

        assertThat(result.getClaimableCount()).isZero();
        assertThat(result.getExpiredCount()).isZero();
    }

    @Test
    @DisplayName("연령이 지났고 소급 기간이 남았으면 신청 가능으로 분류한다")
    void detectsClaimableBenefit() {
        givenChildAgedMonths(30); // 23개월 상한을 7개월 지남
        givenPolicies(policy("부모급여", 0, 23, 350000, 12));

        MissedBenefitSummaryResponse result = service.findMissedBenefits();

        assertThat(result.getClaimableCount()).isEqualTo(1);
        assertThat(result.getClaimableAmount()).isEqualTo(350000);
        assertThat(result.getClaimable().get(0).getRemainingMonths()).isEqualTo(5);
    }

    @Test
    @DisplayName("소급 기간이 지났으면 만료로 분류한다")
    void detectsExpiredBenefit() {
        givenChildAgedMonths(40); // 상한을 17개월 지남, 소급은 12개월까지
        givenPolicies(policy("부모급여", 0, 23, 350000, 12));

        MissedBenefitSummaryResponse result = service.findMissedBenefits();

        assertThat(result.getClaimableCount()).isZero();
        assertThat(result.getExpiredCount()).isEqualTo(1);
        assertThat(result.getExpired().get(0).getReasons())
                .anyMatch(r -> r.contains("소급 가능 기간"));
    }

    @Test
    @DisplayName("소급을 받지 않는 정책은 만료로 둔다")
    void treatsNonRetroactiveAsExpired() {
        givenChildAgedMonths(30);
        givenPolicies(policy("일회성지원", 0, 23, 100000, null));

        MissedBenefitSummaryResponse result = service.findMissedBenefits();

        assertThat(result.getExpiredCount()).isEqualTo(1);
        assertThat(result.getExpired().get(0).getReasons())
                .anyMatch(r -> r.contains("소급 신청을 받지 않습니다"));
    }

    @Test
    @DisplayName("소득 조건 초과자는 목록에서 제외한다")
    void excludesWhenIncomeExceedsThreshold() {
        user.setIncomePercent(200);
        givenChildAgedMonths(30);
        Policy p = policy("저소득지원", 0, 23, 500000, 12);
        p.setIncomeThresholdPercent(150);
        givenPolicies(p);

        MissedBenefitSummaryResponse result = service.findMissedBenefits();

        assertThat(result.getClaimableCount()).isZero();
        assertThat(result.getExpiredCount()).isZero();
    }

    @Test
    @DisplayName("소득 미입력은 제외하지 않고 보류로 표시한다")
    void keepsUnknownIncomeAsPending() {
        givenChildAgedMonths(30); // incomePercent 미입력
        Policy p = policy("저소득지원", 0, 23, 500000, 12);
        p.setIncomeThresholdPercent(150);
        givenPolicies(p);

        MissedBenefitSummaryResponse result = service.findMissedBenefits();

        assertThat(result.getClaimableCount()).isEqualTo(1);
        assertThat(result.getUnknownEligibilityCount()).isEqualTo(1);
        assertThat(result.getClaimable().get(0).getReasons())
                .anyMatch(r -> r.contains("소득 정보를 입력하면"));
    }

    @Test
    @DisplayName("자녀 수 요건을 못 채우면 제외한다")
    void excludesWhenNotEnoughChildren() {
        givenChildAgedMonths(30); // 자녀 1명
        Policy p = policy("다자녀지원", 0, 23, 300000, 12);
        p.setMinChildren(2);
        givenPolicies(p);

        MissedBenefitSummaryResponse result = service.findMissedBenefits();

        assertThat(result.getClaimableCount()).isZero();
    }

    @Test
    @DisplayName("정책 신청 기간이 끝났으면 소급 기간이 남아도 만료다")
    void expiredWhenApplicationWindowClosed() {
        givenChildAgedMonths(30);
        Policy p = policy("종료된지원", 0, 23, 200000, 12);
        p.setApplicationEndDate(LocalDate.now().minusDays(1));
        givenPolicies(p);

        MissedBenefitSummaryResponse result = service.findMissedBenefits();

        assertThat(result.getExpiredCount()).isEqualTo(1);
        assertThat(result.getExpired().get(0).getReasons())
                .anyMatch(r -> r.contains("신청 기간이 종료"));
    }

    @Test
    @DisplayName("금액이 큰 순으로 정렬한다")
    void sortsByAmountDescending() {
        givenChildAgedMonths(30);
        givenPolicies(
                policy("소액", 0, 23, 100000, 12),
                policy("고액", 0, 23, 900000, 12));

        MissedBenefitSummaryResponse result = service.findMissedBenefits();

        assertThat(result.getClaimable().get(0).getTitle()).isEqualTo("고액");
        assertThat(result.getClaimableAmount()).isEqualTo(1_000_000);
    }

    @Test
    @DisplayName("아이가 없으면 조회하지 않는다")
    void returnsEmptyWithoutChildren() {
        when(childRepository.findByUserIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());

        MissedBenefitSummaryResponse result = service.findMissedBenefits();

        assertThat(result.getClaimableCount()).isZero();
        assertThat(result.getClaimable()).isEmpty();
    }

    private void givenChildAgedMonths(int months) {
        Child child = Child.builder()
                .name("아이")
                .birthDate(LocalDate.now().minusMonths(months))
                .build();
        when(childRepository.findByUserIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of(child));
    }

    private void givenPolicies(Policy... policies) {
        Page<Policy> page = new PageImpl<>(List.of(policies));
        when(policyRepository.findByIsActiveTrueOrderByPriorityDescViewCountDesc(any())).thenReturn(page);
    }

    private Policy policy(String title, Integer ageMin, Integer ageMax, Integer amount, Integer retroactive) {
        Policy p = new Policy();
        p.setId(1L);
        p.setTitle(title);
        p.setTargetAgeMin(ageMin);
        p.setTargetAgeMax(ageMax);
        p.setBenefitAmount(amount);
        p.setRetroactiveMonths(retroactive);
        p.setIsActive(true);
        return p;
    }
}
