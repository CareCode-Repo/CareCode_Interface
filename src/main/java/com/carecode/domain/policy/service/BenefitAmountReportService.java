package com.carecode.domain.policy.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.policy.dto.request.BenefitAmountReportRequest;
import com.carecode.domain.policy.dto.response.BenefitAmountConsensusResponse;
import com.carecode.domain.policy.entity.BenefitAmountReport;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.BenefitAmountReportRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 실수령액 제보를 모아 금액을 확정한다.
 * 공공데이터는 지원금액을 숫자로 주지 않아 수기 검증이 유일한 대안인데, 10,964건을 사람이 다 볼 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenefitAmountReportService {

    /** 이 수 이상이 같은 값을 내면 채택한다. 낮추면 오류가, 높이면 영영 안 채워진다. */
    @Value("${app.benefit-report.consensus-threshold:3}")
    private int consensusThreshold;

    private final BenefitAmountReportRepository reportRepository;
    private final PolicyRepository policyRepository;
    private final CurrentUserFacade currentUserFacade;

    @Transactional
    public BenefitAmountConsensusResponse report(Long policyId, BenefitAmountReportRequest request) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new CareServiceException("정책을 찾을 수 없습니다: " + policyId));
        User user = currentUserFacade.requireCurrentUser();

        BenefitAmountReport.PaymentType type =
                BenefitAmountReport.PaymentType.valueOf(request.getPaymentType());

        // 같은 사람이 여러 번 내면 표본이 왜곡되므로 갱신으로 처리한다.
        reportRepository.findByPolicyIdAndUserId(policyId, user.getId())
                .ifPresentOrElse(
                        existing -> existing.update(request.getAmount(), type,
                                request.getReceivedAt(), request.getNote()),
                        () -> reportRepository.save(BenefitAmountReport.builder()
                                .policyId(policyId)
                                .user(user)
                                .reportedAmount(request.getAmount())
                                .paymentType(type)
                                .receivedAt(request.getReceivedAt())
                                .note(request.getNote())
                                .createdAt(LocalDateTime.now())
                                .build()));

        return evaluateConsensus(policy);
    }

    @Transactional(readOnly = true)
    public BenefitAmountConsensusResponse getConsensus(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new CareServiceException("정책을 찾을 수 없습니다: " + policyId));
        return buildResponse(policy, findTopReport(policyId));
    }

    /** 합의가 이뤄지면 정책 금액을 채우고 검증 표시를 남긴다. */
    private BenefitAmountConsensusResponse evaluateConsensus(Policy policy) {
        TopReport top = findTopReport(policy.getId());

        if (top != null && top.count() >= consensusThreshold && policy.getVerifiedAt() == null) {
            policy.setBenefitAmount(top.amount());
            policy.setBenefitType(top.paymentType() == BenefitAmountReport.PaymentType.MONTHLY
                    ? "월지급" : "일시지급");
            policy.setVerifiedAt(LocalDateTime.now());
            policy.setVerifiedBy("제보 합의 " + top.count() + "명");
            policyRepository.save(policy);

            log.info("제보 합의로 금액 확정 - policyId={}, amount={}, 제보 {}명",
                    policy.getId(), top.amount(), top.count());
        }
        return buildResponse(policy, top);
    }

    private record TopReport(int amount, BenefitAmountReport.PaymentType paymentType, long count) {
    }

    private TopReport findTopReport(Long policyId) {
        List<Object[]> rows = reportRepository.countByAmountAndType(policyId);
        if (rows.isEmpty()) {
            return null;
        }
        Object[] top = rows.get(0);
        return new TopReport((Integer) top[0], (BenefitAmountReport.PaymentType) top[1], (Long) top[2]);
    }

    private BenefitAmountConsensusResponse buildResponse(Policy policy, TopReport top) {
        long totalReports = reportRepository.findByPolicyId(policy.getId()).size();

        return BenefitAmountConsensusResponse.builder()
                .policyId(policy.getId())
                .title(policy.getTitle())
                .totalReports(totalReports)
                .consensusThreshold(consensusThreshold)
                .agreedCount(top == null ? 0 : top.count())
                .consensusAmount(top == null ? null : top.amount())
                .consensusPaymentType(top == null ? null : top.paymentType().name())
                .confirmed(policy.getVerifiedAt() != null)
                .currentAmount(policy.getBenefitAmount())
                .build();
    }
}
