package com.carecode.core.analytics;

import com.carecode.core.analytics.dto.FunnelResponse;
import com.carecode.core.analytics.dto.RetentionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("퍼널·리텐션 집계")
class AnalyticsServiceTest {

    private UserEventRepository repository;
    private AnalyticsService service;
    private final LocalDate from = LocalDate.now().minusDays(30);
    private final LocalDate to = LocalDate.now();

    @BeforeEach
    void setUp() {
        repository = mock(UserEventRepository.class);
        service = new AnalyticsService(repository);
    }

    @Test
    @DisplayName("퍼널은 앞 단계를 거친 사용자만 세어 전환율을 낸다")
    void countsOnlyConvertedUsers() {
        when(repository.countDistinctUsers(eq(EventType.SIGNED_UP), any(), any())).thenReturn(100L);
        when(repository.countConverted(eq(EventType.SIGNED_UP), eq(EventType.CHILD_REGISTERED), any(), any()))
                .thenReturn(60L);
        when(repository.countConverted(eq(EventType.CHILD_REGISTERED), eq(EventType.MISSED_BENEFIT_VIEWED), any(), any()))
                .thenReturn(30L);
        when(repository.countConverted(eq(EventType.MISSED_BENEFIT_VIEWED), eq(EventType.BENEFIT_LINK_CLICKED), any(), any()))
                .thenReturn(9L);

        FunnelResponse result = service.funnel(from, to);

        assertThat(result.getSteps()).hasSize(4);
        assertThat(result.getSteps().get(0).getUsers()).isEqualTo(100);
        assertThat(result.getSteps().get(0).getConversionRate()).isNull();
        assertThat(result.getSteps().get(1).getConversionRate()).isEqualTo(60);
        assertThat(result.getSteps().get(2).getConversionRate()).isEqualTo(50);
        // 신청 링크 클릭 = 서비스가 실제로 돈을 찾아줬는지 증명하는 지표
        assertThat(result.getSteps().get(3).getConversionRate()).isEqualTo(30);
    }

    @Test
    @DisplayName("앞 단계가 0명이면 전환율을 0으로 둔다")
    void handlesEmptyFunnel() {
        when(repository.countDistinctUsers(any(), any(), any())).thenReturn(0L);
        when(repository.countConverted(any(), any(), any(), any())).thenReturn(0L);

        FunnelResponse result = service.funnel(from, to);

        assertThat(result.getSteps().get(1).getConversionRate()).isZero();
    }

    @Test
    @DisplayName("리텐션은 가입일 코호트별로 잔존율을 계산한다")
    void calculatesCohortRetention() {
        LocalDate signUp = LocalDate.now().minusDays(40);
        when(repository.findUserIdsSignedUpOn(any())).thenReturn(List.of());
        when(repository.findUserIdsSignedUpOn(signUp)).thenReturn(List.of(1L, 2L, 3L, 4L));
        when(repository.countActiveOn(anyList(), eq(signUp.plusDays(1)))).thenReturn(2L);
        when(repository.countActiveOn(anyList(), eq(signUp.plusDays(7)))).thenReturn(1L);
        when(repository.countActiveOn(anyList(), eq(signUp.plusDays(30)))).thenReturn(1L);

        RetentionResponse result = service.retention(LocalDate.now().minusDays(45), LocalDate.now());

        RetentionResponse.Cohort cohort = result.getCohorts().stream()
                .filter(c -> c.getSignUpDate().equals(signUp)).findFirst().orElseThrow();
        assertThat(cohort.getSignedUp()).isEqualTo(4);
        assertThat(cohort.getDay1()).isEqualTo(50);
        assertThat(cohort.getDay7()).isEqualTo(25);
        assertThat(cohort.getDay30()).isEqualTo(25);
    }

    @Test
    @DisplayName("아직 오지 않은 날짜는 0%가 아니라 미집계로 둔다")
    void marksFutureCheckpointsAsUnknown() {
        LocalDate signUp = LocalDate.now().minusDays(2);
        when(repository.findUserIdsSignedUpOn(any())).thenReturn(List.of());
        when(repository.findUserIdsSignedUpOn(signUp)).thenReturn(List.of(1L, 2L));
        when(repository.countActiveOn(anyList(), any())).thenReturn(1L);

        RetentionResponse result = service.retention(LocalDate.now().minusDays(5), LocalDate.now());

        RetentionResponse.Cohort cohort = result.getCohorts().stream()
                .filter(c -> c.getSignUpDate().equals(signUp)).findFirst().orElseThrow();
        assertThat(cohort.getDay1()).isEqualTo(50);
        // D7·D30 은 아직 도래하지 않았다 — 0% 로 보이면 리텐션이 폭락한 것처럼 왜곡된다
        assertThat(cohort.getDay7()).isNull();
        assertThat(cohort.getDay30()).isNull();
    }

    @Test
    @DisplayName("가입자가 없는 날짜는 코호트에서 제외한다")
    void skipsEmptyCohorts() {
        when(repository.findUserIdsSignedUpOn(any())).thenReturn(List.of());

        assertThat(service.retention(from, to).getCohorts()).isEmpty();
    }
}
