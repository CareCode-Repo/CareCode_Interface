package com.carecode.domain.policy.service;

import com.carecode.core.benefit.BenefitProjectionCalculator;
import com.carecode.core.exception.CareServiceException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.policy.dto.response.RegionalBenefitComparisonResponse;
import com.carecode.domain.policy.dto.response.RegionalBenefitResponse;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("거주지별 지원금 비교")
class RegionalBenefitComparisonServiceTest {

    private PolicyRepository policyRepository;
    private ChildRepository childRepository;
    private RegionalBenefitComparisonService service;
    private User user;

    @BeforeEach
    void setUp() {
        policyRepository = mock(PolicyRepository.class);
        childRepository = mock(ChildRepository.class);
        CurrentUserFacade currentUserFacade = mock(CurrentUserFacade.class);

        user = User.builder().id(1L).name("부모").address("경기도 성남시 분당구").build();
        when(currentUserFacade.requireCurrentUser()).thenReturn(user);

        service = new RegionalBenefitComparisonService(
                policyRepository, childRepository, currentUserFacade, new BenefitProjectionCalculator());
    }

    @Test
    @DisplayName("전국 정책은 모든 지역에 공통으로 더한다")
    void addsNationwidePolicyToEveryRegion() {
        givenChildAgedMonths(0);
        givenPolicies(
                policy("부모급여", "전국", 0, 11, 1000000, "월지급"),
                policy("성남시 출산장려금", "성남시", 0, 11, 500000, "일시지급"),
                policy("OO군 출산장려금", "OO군", 0, 11, 3000000, "일시지급"));
        givenRegions("성남시", "OO군");

        RegionalBenefitComparisonResponse result = service.compare(null, 1, 10);

        // 전국 1,000,000 × 12개월 = 12,000,000 이 두 지역 모두에 포함된다
        assertThat(byRegion(result, "성남시").getTotalAmount()).isEqualTo(12_000_000 + 500_000);
        assertThat(byRegion(result, "OO군").getTotalAmount()).isEqualTo(12_000_000 + 3_000_000);
    }

    @Test
    @DisplayName("현재 거주지 대비 차액을 계산한다")
    void calculatesDifferenceFromCurrentRegion() {
        givenChildAgedMonths(0);
        givenPolicies(
                policy("성남시 지원", "성남시", 0, 11, 500000, "일시지급"),
                policy("OO군 지원", "OO군", 0, 11, 3000000, "일시지급"));
        givenRegions("성남시", "OO군");

        RegionalBenefitComparisonResponse result = service.compare(null, 1, 10);

        assertThat(result.getBaseRegion()).isEqualTo("성남시");
        assertThat(result.getBaseAmount()).isEqualTo(500_000);
        assertThat(byRegion(result, "OO군").getDifferenceFromBase()).isEqualTo(2_500_000);
        assertThat(byRegion(result, "성남시").getDifferenceFromBase()).isZero();
    }

    @Test
    @DisplayName("총액 내림차순으로 정렬한다")
    void sortsByTotalDescending() {
        givenChildAgedMonths(0);
        givenPolicies(
                policy("소액", "A시", 0, 11, 100000, "일시지급"),
                policy("고액", "B군", 0, 11, 5000000, "일시지급"));
        givenRegions("A시", "B군");

        RegionalBenefitComparisonResponse result = service.compare(null, 1, 10);

        assertThat(result.getRankings().get(0).getRegion()).isEqualTo("B군");
    }

    @Test
    @DisplayName("주소가 없으면 전국 정책만을 기준으로 삼는다")
    void fallsBackToNationwideWhenAddressMissing() {
        user.setAddress(null);
        givenChildAgedMonths(0);
        givenPolicies(
                policy("전국지원", "전국", 0, 11, 100000, "월지급"),
                policy("OO군 지원", "OO군", 0, 11, 3000000, "일시지급"));
        givenRegions("OO군");

        RegionalBenefitComparisonResponse result = service.compare(null, 1, 10);

        assertThat(result.getBaseRegion()).isNull();
        assertThat(result.getBaseAmount()).isEqualTo(1_200_000); // 전국 정책만
        assertThat(result.getDisclaimers()).anyMatch(d -> d.contains("주소가 등록되지 않아"));
    }

    @Test
    @DisplayName("현금이 아닌 혜택은 금액이 아니라 건수로 센다")
    void countsNonCashSeparately() {
        givenChildAgedMonths(0);
        givenPolicies(
                policy("무료검진", "A시", 0, 11, 300000, "무료검진"),
                policy("현금지원", "A시", 0, 11, 200000, "일시지급"));
        givenRegions("A시");

        RegionalBenefitResponse a = byRegion(service.compare(null, 1, 10), "A시");

        assertThat(a.getTotalAmount()).isEqualTo(200_000);
        assertThat(a.getCashPolicyCount()).isEqualTo(1);
        assertThat(a.getNonCashPolicyCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("금액 기여가 큰 정책을 근거로 노출한다")
    void exposesTopContributors() {
        givenChildAgedMonths(0);
        givenPolicies(
                policy("소액", "A시", 0, 11, 100000, "일시지급"),
                policy("고액", "A시", 0, 11, 900000, "일시지급"));
        givenRegions("A시");

        RegionalBenefitResponse a = byRegion(service.compare(null, 1, 10), "A시");

        assertThat(a.getTopContributors().get(0).getTitle()).isEqualTo("고액");
    }

    @Test
    @DisplayName("상위 노출 개수를 제한한다")
    void limitsRankingSize() {
        givenChildAgedMonths(0);
        givenPolicies(
                policy("a", "A시", 0, 11, 100000, "일시지급"),
                policy("b", "B시", 0, 11, 200000, "일시지급"),
                policy("c", "C시", 0, 11, 300000, "일시지급"));
        givenRegions("A시", "B시", "C시");

        assertThat(service.compare(null, 1, 2).getRankings()).hasSize(2);
    }

    @Test
    @DisplayName("추정치임을 항상 알린다")
    void alwaysMarksAsEstimate() {
        givenChildAgedMonths(0);
        givenPolicies(policy("지원", "A시", 0, 11, 100000, "일시지급"));
        givenRegions("A시");

        RegionalBenefitComparisonResponse result = service.compare(null, 1, 10);

        assertThat(result.getDataQuality()).isEqualTo("ESTIMATED");
        assertThat(result.getDisclaimers()).anyMatch(d -> d.contains("추정치"));
    }

    @Test
    @DisplayName("자녀가 없으면 계산할 수 없다고 알린다")
    void failsWithoutChild() {
        when(childRepository.findByUserIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());

        assertThatThrownBy(() -> service.compare(null, 5, 10))
                .isInstanceOf(CareServiceException.class)
                .hasMessageContaining("등록된 자녀가 없습니다");
    }

    @Test
    @DisplayName("생년월일이 없으면 계산할 수 없다고 알린다")
    void failsWithoutBirthDate() {
        Child child = Child.builder().name("아이").birthDate(null).build();
        when(childRepository.findByUserIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of(child));

        assertThatThrownBy(() -> service.compare(null, 5, 10))
                .isInstanceOf(CareServiceException.class)
                .hasMessageContaining("생년월일");
    }

    private void givenChildAgedMonths(int months) {
        Child child = Child.builder()
                .id(1L).name("아이")
                .birthDate(LocalDate.now().minusMonths(months))
                .build();
        when(childRepository.findByUserIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of(child));
    }

    private void givenPolicies(Policy... policies) {
        when(policyRepository.findByIsActiveTrue()).thenReturn(List.of(policies));
    }

    private void givenRegions(String... regions) {
        when(policyRepository.findDistinctTargetRegions()).thenReturn(List.of(regions));
    }

    private RegionalBenefitResponse byRegion(RegionalBenefitComparisonResponse response, String region) {
        return response.getRankings().stream()
                .filter(r -> r.getRegion().equals(region))
                .findFirst()
                .orElseThrow(() -> new AssertionError(region + " 결과가 없습니다"));
    }

    private Policy policy(String title, String region, Integer ageMin, Integer ageMax,
                          Integer amount, String benefitType) {
        Policy p = new Policy();
        p.setTitle(title);
        p.setTargetRegion(region);
        p.setTargetAgeMin(ageMin);
        p.setTargetAgeMax(ageMax);
        p.setBenefitAmount(amount);
        p.setBenefitType(benefitType);
        p.setIsActive(true);
        return p;
    }
}
