package com.carecode.domain.policy.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.notification.sender.NotificationDispatcher;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyDeadlineNotice;
import com.carecode.domain.policy.repository.PolicyDeadlineNoticeRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 신청 마감이 다가온 지원금을 대상자에게 미리 알린다.
 *
 * <p>{@link MissedBenefitService} 는 이미 놓친 것을 사후에 알려준다. 놓치기 전에 막는 쪽이
 * 훨씬 낫고, 사용자가 실제로 돈을 받게 되는 순간이 이 서비스의 유일한 증명이다.
 *
 * <p>중복 발송은 두 겹으로 막는다. 마감일까지 남은 일수가 지정한 값과 정확히 같은 날에만
 * 보내고, 그날 이미 보낸 사람은 발송 이력으로 걸러낸다. 남은 일수만으로는 스케줄러가 하루에
 * 두 번 돌거나 배포 중 인스턴스가 두 대일 때를 막지 못한다. 지원금 알림은 한 번 더 오는
 * 순간 신뢰를 잃는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyDeadlineNotifier {

    private static final int CANDIDATE_SIZE = 500;

    /**
     * 마감 며칠 전에 알릴지. 기본은 D-7 과 D-1 이다.
     *
     * <p>D-7 은 서류를 준비할 시간을 주고, D-1 은 그날 스케줄러가 실패했거나 알림을 놓친
     * 사람에게 마지막 기회가 된다.
     */
    @Value("${app.policy-deadline.lead-days:7,1}")
    private String leadDaysRaw;

    private final PolicyRepository policyRepository;
    private final PolicyDeadlineNoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher dispatcher;
    private final EventLogger eventLogger;

    @Getter
    public static class DeadlineNotifyResult {
        private int policiesDueSoon;
        private int notificationsSent;

        @Override
        public String toString() {
            return String.format("마감 임박 정책 %d건, 알림 %d건 발송", policiesDueSoon, notificationsSent);
        }
    }

    @Transactional
    public DeadlineNotifyResult notifyUpcomingDeadlines() {
        DeadlineNotifyResult result = new DeadlineNotifyResult();
        Set<Integer> leadDays = parseLeadDays();
        if (leadDays.isEmpty()) {
            return result;
        }

        LocalDate today = LocalDate.now();
        List<Policy> candidates = policyRepository
                .findByIsActiveTrueOrderByPriorityDescViewCountDesc(PageRequest.of(0, CANDIDATE_SIZE))
                .getContent();

        List<Policy> dueSoon = candidates.stream()
                .filter(p -> p.getApplicationEndDate() != null)
                .filter(p -> leadDays.contains((int) ChronoUnit.DAYS.between(today, p.getApplicationEndDate())))
                .toList();

        if (dueSoon.isEmpty()) {
            return result;
        }
        result.policiesDueSoon = dueSoon.size();

        List<User> activeUsers = userRepository.findByIsActiveTrue();
        for (Policy policy : dueSoon) {
            try {
                result.notificationsSent += notify(policy, activeUsers, today);
            } catch (Exception e) {
                // 한 정책의 실패가 나머지 마감 알림을 막아서는 안 된다.
                log.warn("마감 임박 알림 실패 - policyId={}, 사유={}", policy.getId(), e.getMessage());
            }
        }

        log.info("마감 임박 알림 - {}", result);
        return result;
    }

    private int notify(Policy policy, List<User> activeUsers, LocalDate today) {
        int daysLeft = (int) ChronoUnit.DAYS.between(today, policy.getApplicationEndDate());
        String title = String.format("신청 마감 %s: %s", daysLeft <= 1 ? "내일" : "D-" + daysLeft, policy.getTitle());
        String message = buildMessage(policy, daysLeft);

        Set<Long> alreadyNotified = Set.copyOf(
                noticeRepository.findNotifiedUserIds(policy.getId(), today));

        int sent = 0;
        for (User user : activeUsers) {
            if (alreadyNotified.contains(user.getId())) {
                continue;
            }
            if (!isTarget(policy, user, today)) {
                continue;
            }

            // 보낸 사실을 먼저 남긴다. 유니크 제약이 인스턴스가 둘일 때도 한 번만 남게 만든다.
            noticeRepository.save(PolicyDeadlineNotice.builder()
                    .policyId(policy.getId())
                    .userId(user.getId())
                    .notifiedOn(today)
                    .daysLeft(daysLeft)
                    .build());

            Notification notification = notificationRepository.save(Notification.builder()
                    .user(user)
                    .notificationType(Notification.NotificationType.POLICY)
                    .title(title)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build());

            dispatcher.dispatchAsync(notification);
            eventLogger.log(EventType.NOTIFICATION_SENT, user.getId(),
                    String.valueOf(notification.getId()), "POLICY_DEADLINE");
            sent++;
        }
        return sent;
    }

    /**
     * 대상자인지 판단한다.
     *
     * <p>마감 알림은 성격상 조금 넓게 보내는 편이 낫다. 놓친 사람의 손해가 잘못 받은 알림의
     * 성가심보다 훨씬 크기 때문에, 소득 미입력처럼 판단할 수 없는 경우는 배제하지 않는다.
     * 다만 자녀가 없거나 지역·연령이 명확히 어긋나면 보내지 않는다.
     */
    private boolean isTarget(Policy policy, User user, LocalDate today) {
        List<Child> children = childRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (children.isEmpty()) {
            return false;
        }
        if (!matchesRegion(policy, user)) {
            return false;
        }

        Integer minChildren = policy.getMinChildren();
        if (minChildren != null && children.size() < minChildren) {
            return false;
        }

        Integer threshold = policy.getIncomeThresholdPercent();
        Integer income = user.getIncomePercent();
        // 소득 미입력을 탈락으로 처리하면 받을 수 있었던 지원금이 통째로 사라진다.
        if (threshold != null && income != null && income > threshold) {
            return false;
        }

        return children.stream().anyMatch(child -> matchesAge(policy, child, today));
    }

    /** 전국 정책은 모두에게, 지역 정책은 그 지역 주민에게만. */
    private boolean matchesRegion(Policy policy, User user) {
        String region = policy.getTargetRegion();
        if (region == null || region.isBlank() || region.contains("전국")) {
            return true;
        }
        String address = user.getAddress();
        if (address == null || address.isBlank()) {
            return false;
        }
        // 주소는 "충청북도 청주시 ...", 정책 지역은 "충청북도 청주시" 처럼 표기가 달라 양방향으로 본다.
        return address.contains(region) || region.contains(address);
    }

    private boolean matchesAge(Policy policy, Child child, LocalDate today) {
        if (child.getBirthDate() == null) {
            // 생일을 모르면 연령으로 배제하지 않는다.
            return true;
        }
        int months = (int) ChronoUnit.MONTHS.between(child.getBirthDate(), today);

        Integer min = policy.getTargetAgeMin();
        if (min != null && months < min) {
            return false;
        }
        Integer max = policy.getTargetAgeMax();
        return max == null || months <= max;
    }

    private String buildMessage(Policy policy, int daysLeft) {
        String when = daysLeft <= 1
                ? "내일(" + policy.getApplicationEndDate() + ") 마감됩니다."
                : policy.getApplicationEndDate() + "에 마감됩니다. (" + daysLeft + "일 남음)";

        String amount = policy.getBenefitAmount() != null && policy.getBenefitAmount() > 0
                ? String.format(" 지원금액은 %,d원으로 등록되어 있습니다.", policy.getBenefitAmount())
                : "";

        // 신청 자체는 정부 사이트에서 해야 하므로 기대를 정확히 맞춰 준다.
        return String.format("%s 신청이 %s%s 대상 여부와 서류를 지금 확인해 보세요.",
                policy.getTitle(), when, amount);
    }

    private Set<Integer> parseLeadDays() {
        try {
            return Arrays.stream(leadDaysRaw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .filter(d -> d >= 0)
                    .collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            log.warn("app.policy-deadline.lead-days 설정이 잘못되어 마감 알림을 건너뜁니다: {}", leadDaysRaw);
            return Set.of();
        }
    }
}
