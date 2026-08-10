package com.carecode.domain.policy.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.notification.sender.NotificationDispatcher;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.BenefitAmountReportRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 받았을 법한 사람에게 실수령액을 묻는다.
 * 아무에게나 물으면 소음이지만, 대상 연령을 막 지난 사람은 방금 받아봤을 가능성이 높다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenefitReportSolicitor {

    /** 대상 연령이 지난 뒤 이 기간 안에 있는 사람에게만 묻는다. 너무 오래되면 기억이 흐려진다. */
    @Value("${app.benefit-report.ask-within-months:6}")
    private int askWithinMonths;

    /** 한 번에 한 사람에게 보낼 최대 질문 수. 여러 건을 한꺼번에 물으면 아무것도 답하지 않는다. */
    @Value("${app.benefit-report.max-asks-per-user:1}")
    private int maxAsksPerUser;

    private final PolicyRepository policyRepository;
    private final BenefitAmountReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher dispatcher;
    private final EventLogger eventLogger;

    @Getter
    public static class SolicitResult {
        private int usersAsked;
        private int questionsSent;

        @Override
        public String toString() {
            return String.format("%d명에게 %d건 질문", usersAsked, questionsSent);
        }
    }

    @Transactional
    public SolicitResult solicitReports() {
        SolicitResult result = new SolicitResult();

        // 금액이 이미 확인된 정책은 물을 이유가 없다.
        List<Policy> unknownAmount = policyRepository.findByIsActiveTrue().stream()
                .filter(p -> p.getBenefitAmount() == null || p.getBenefitAmount() <= 0)
                .filter(p -> p.getVerifiedAt() == null)
                .toList();

        if (unknownAmount.isEmpty()) {
            return result;
        }

        for (User user : userRepository.findByIsActiveTrue()) {
            List<Policy> candidates = findRecentlyPassed(user, unknownAmount);
            if (candidates.isEmpty()) {
                continue;
            }

            int asked = 0;
            for (Policy policy : candidates) {
                if (asked >= maxAsksPerUser) {
                    break;
                }
                // 이미 답한 사람에게 다시 묻지 않는다.
                if (reportRepository.findByPolicyIdAndUserId(policy.getId(), user.getId()).isPresent()) {
                    continue;
                }
                ask(user, policy);
                asked++;
                result.questionsSent++;
            }
            if (asked > 0) {
                result.usersAsked++;
            }
        }

        log.info("실수령액 제보 요청 - {}", result);
        return result;
    }

    /** 아이가 대상 연령을 최근에 지난 정책. 방금 받아봤을 가능성이 높은 구간이다. */
    private List<Policy> findRecentlyPassed(User user, List<Policy> policies) {
        List<Child> children = childRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (children.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        List<Policy> matched = new ArrayList<>();

        for (Policy policy : policies) {
            if (policy.getTargetAgeMax() == null || !matchesRegion(policy, user)) {
                continue;
            }
            boolean recentlyPassed = children.stream().anyMatch(child -> {
                if (child.getBirthDate() == null) {
                    return false;
                }
                long months = ChronoUnit.MONTHS.between(child.getBirthDate(), today);
                long sincePassed = months - policy.getTargetAgeMax();
                return sincePassed > 0 && sincePassed <= askWithinMonths;
            });
            if (recentlyPassed) {
                matched.add(policy);
            }
        }
        return matched;
    }

    private boolean matchesRegion(Policy policy, User user) {
        String region = policy.getTargetRegion();
        if (region == null || region.isBlank() || region.contains("전국")) {
            return true;
        }
        String address = user.getAddress();
        return address != null && (address.contains(region) || region.contains(address));
    }

    private void ask(User user, Policy policy) {
        Notification notification = notificationRepository.save(Notification.builder()
                .user(user)
                .notificationType(Notification.NotificationType.POLICY)
                .title("혹시 이 지원금 받으셨나요?")
                .message(String.format(
                        "'%s' 의 실제 수령액을 알려주시면 같은 지역 부모들에게 정확한 정보가 전달됩니다. "
                                + "30초면 됩니다.", policy.getTitle()))
                .createdAt(LocalDateTime.now())
                .build());

        dispatcher.dispatchAsync(notification);
        eventLogger.log(EventType.NOTIFICATION_SENT, user.getId(),
                String.valueOf(notification.getId()), "BENEFIT_REPORT_ASK");
    }
}
