package com.carecode.domain.policy.service;

import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyChange;
import com.carecode.domain.policy.repository.PolicyChangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("정책 변경 감지")
class PolicyChangeDetectorTest {

    private PolicyChangeRepository repository;
    private PolicyChangeDetector detector;

    @BeforeEach
    void setUp() {
        repository = mock(PolicyChangeRepository.class);
        detector = new PolicyChangeDetector(repository);
    }

    @Test
    @DisplayName("신규 정책을 기록한다")
    void recordsCreation() {
        Policy policy = policy(100000, LocalDate.of(2026, 12, 31), 0, 11);

        detector.recordCreated(policy);

        PolicyChange saved = capture();
        assertThat(saved.getChangeType()).isEqualTo(PolicyChange.ChangeType.CREATED);
        assertThat(saved.getTargetRegion()).isEqualTo("성남시");
    }

    @Test
    @DisplayName("금액 변경을 기록한다")
    void recordsAmountChange() {
        Policy policy = policy(300000, LocalDate.of(2026, 12, 31), 0, 11);
        var before = new PolicyChangeDetector.Before(100000, LocalDate.of(2026, 12, 31), 0, 11);

        detector.recordUpdates(policy, before);

        PolicyChange saved = capture();
        assertThat(saved.getChangeType()).isEqualTo(PolicyChange.ChangeType.AMOUNT_CHANGED);
        assertThat(saved.getOldValue()).isEqualTo("100000");
        assertThat(saved.getNewValue()).isEqualTo("300000");
    }

    @Test
    @DisplayName("신청기한 변경을 기록한다")
    void recordsDeadlineChange() {
        Policy policy = policy(100000, LocalDate.of(2027, 6, 30), 0, 11);
        var before = new PolicyChangeDetector.Before(100000, LocalDate.of(2026, 12, 31), 0, 11);

        detector.recordUpdates(policy, before);

        assertThat(capture().getChangeType()).isEqualTo(PolicyChange.ChangeType.DEADLINE_CHANGED);
    }

    @Test
    @DisplayName("대상 연령 변경을 기록한다")
    void recordsAgeRangeChange() {
        Policy policy = policy(100000, LocalDate.of(2026, 12, 31), 0, 23);
        var before = new PolicyChangeDetector.Before(100000, LocalDate.of(2026, 12, 31), 0, 11);

        detector.recordUpdates(policy, before);

        PolicyChange saved = capture();
        assertThat(saved.getChangeType()).isEqualTo(PolicyChange.ChangeType.AGE_RANGE_CHANGED);
        assertThat(saved.getNewValue()).isEqualTo("0~23개월");
    }

    @Test
    @DisplayName("바뀐 게 없으면 기록하지 않는다")
    void recordsNothingWhenUnchanged() {
        Policy policy = policy(100000, LocalDate.of(2026, 12, 31), 0, 11);
        var before = new PolicyChangeDetector.Before(100000, LocalDate.of(2026, 12, 31), 0, 11);

        detector.recordUpdates(policy, before);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("금액이 null 로 바뀌는 것은 알리지 않는다")
    void ignoresAmountBecomingNull() {
        // 동기화가 금액을 못 읽은 경우까지 "금액 변경" 으로 알리면 소음이 된다
        Policy policy = policy(null, LocalDate.of(2026, 12, 31), 0, 11);
        var before = new PolicyChangeDetector.Before(100000, LocalDate.of(2026, 12, 31), 0, 11);

        detector.recordUpdates(policy, before);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("기록 실패가 동기화를 멈추지 않는다")
    void swallowsSaveFailure() {
        org.mockito.Mockito.when(repository.save(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("DB 오류"));

        // 예외가 밖으로 나오면 동기화 전체가 실패한다
        detector.recordCreated(policy(100000, null, 0, 11));
    }

    private PolicyChange capture() {
        ArgumentCaptor<PolicyChange> captor = ArgumentCaptor.forClass(PolicyChange.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        List<PolicyChange> all = captor.getAllValues();
        return all.get(all.size() - 1);
    }

    private Policy policy(Integer amount, LocalDate endDate, Integer ageMin, Integer ageMax) {
        Policy p = new Policy();
        p.setId(1L);
        p.setTitle("테스트 지원금");
        p.setTargetRegion("성남시");
        p.setBenefitAmount(amount);
        p.setApplicationEndDate(endDate);
        p.setTargetAgeMin(ageMin);
        p.setTargetAgeMax(ageMax);
        return p;
    }
}
