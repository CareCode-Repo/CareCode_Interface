package com.carecode.domain.policy.service;

import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.policy.dto.request.BenefitAmountReportRequest;
import com.carecode.domain.policy.dto.response.BenefitAmountConsensusResponse;
import com.carecode.domain.policy.entity.BenefitAmountReport;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.BenefitAmountReportRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("지원금 실수령액 제보")
class BenefitAmountReportServiceTest {

    private BenefitAmountReportRepository reportRepository;
    private PolicyRepository policyRepository;
    private BenefitAmountReportService service;
    private Policy policy;

    @BeforeEach
    void setUp() {
        reportRepository = mock(BenefitAmountReportRepository.class);
        policyRepository = mock(PolicyRepository.class);
        CurrentUserFacade facade = mock(CurrentUserFacade.class);
        when(facade.requireCurrentUser()).thenReturn(User.builder().id(1L).name("부모").build());

        policy = new Policy();
        policy.setId(10L);
        policy.setTitle("출산장려금");
        when(policyRepository.findById(10L)).thenReturn(Optional.of(policy));
        when(reportRepository.findByPolicyIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(reportRepository.findByPolicyId(anyLong())).thenReturn(List.of());

        service = new BenefitAmountReportService(reportRepository, policyRepository, facade);
        ReflectionTestUtils.setField(service, "consensusThreshold", 3);
    }

    @Test
    @DisplayName("제보 수가 모자라면 확정하지 않는다")
    void doesNotConfirmBelowThreshold() {
        givenConsensus(500000, BenefitAmountReport.PaymentType.ONE_TIME, 2);

        BenefitAmountConsensusResponse result = service.report(10L, request(500000, "ONE_TIME"));

        assertThat(result.isConfirmed()).isFalse();
        assertThat(result.getRemainingForConsensus()).isEqualTo(1);
        assertThat(policy.getBenefitAmount()).isNull();
    }

    @Test
    @DisplayName("3명이 같은 값을 내면 금액을 확정한다")
    void confirmsAtThreshold() {
        givenConsensus(500000, BenefitAmountReport.PaymentType.ONE_TIME, 3);

        BenefitAmountConsensusResponse result = service.report(10L, request(500000, "ONE_TIME"));

        assertThat(result.isConfirmed()).isTrue();
        assertThat(result.getRemainingForConsensus()).isZero();
        assertThat(policy.getBenefitAmount()).isEqualTo(500000);
        assertThat(policy.getBenefitType()).isEqualTo("일시지급");
        assertThat(policy.getVerifiedBy()).contains("제보 합의");
    }

    @Test
    @DisplayName("월 지급이면 지급방식을 월지급으로 확정한다")
    void confirmsMonthlyType() {
        givenConsensus(300000, BenefitAmountReport.PaymentType.MONTHLY, 5);

        service.report(10L, request(300000, "MONTHLY"));

        assertThat(policy.getBenefitType()).isEqualTo("월지급");
    }

    @Test
    @DisplayName("수기 검증된 정책은 제보로 덮어쓰지 않는다")
    void doesNotOverrideManualVerification() {
        policy.setVerifiedAt(java.time.LocalDateTime.now());
        policy.setBenefitAmount(700000);
        givenConsensus(500000, BenefitAmountReport.PaymentType.ONE_TIME, 10);

        service.report(10L, request(500000, "ONE_TIME"));

        // 사람이 공고를 보고 확인한 값이 제보보다 우선한다
        assertThat(policy.getBenefitAmount()).isEqualTo(700000);
    }

    @Test
    @DisplayName("같은 사람이 다시 내면 새로 만들지 않고 갱신한다")
    void updatesInsteadOfDuplicating() {
        BenefitAmountReport existing = mock(BenefitAmountReport.class);
        when(reportRepository.findByPolicyIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(existing));
        givenConsensus(500000, BenefitAmountReport.PaymentType.ONE_TIME, 1);

        service.report(10L, request(600000, "ONE_TIME"));

        verify(existing).update(any(), any(), any(), any());
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("제보가 없으면 합의 값이 비어 있다")
    void emptyWhenNoReports() {
        when(reportRepository.countByAmountAndType(anyLong())).thenReturn(List.of());

        BenefitAmountConsensusResponse result = service.getConsensus(10L);

        assertThat(result.getConsensusAmount()).isNull();
        assertThat(result.getAgreedCount()).isZero();
        assertThat(result.getRemainingForConsensus()).isEqualTo(3);
    }

    private void givenConsensus(int amount, BenefitAmountReport.PaymentType type, long count) {
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{amount, type, count});
        when(reportRepository.countByAmountAndType(anyLong())).thenReturn(rows);
    }

    private BenefitAmountReportRequest request(int amount, String type) {
        BenefitAmountReportRequest r = new BenefitAmountReportRequest();
        r.setAmount(amount);
        r.setPaymentType(type);
        return r;
    }
}
