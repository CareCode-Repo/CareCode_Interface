package com.carecode.domain.policy.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.notification.sender.NotificationDispatcher;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyChange;
import com.carecode.domain.policy.repository.PolicyChangeRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 기록된 정책 변경을 해당 지역 사용자에게 알린다.
 * 이 앱은 "한 번 보고 끝" 이 되기 쉬운데, 다시 열 이유를 만드는 유일한 경로다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyChangeNotifier {

    /** 한 번에 처리할 변경 수. 동기화 직후 수천 건이 쌓여도 알림이 폭주하지 않게 한다. */
    @Value("${app.policy-change.batch-size:200}")
    private int batchSize;

    /** 한 사용자에게 한 번에 보낼 최대 알림 수. 넘치면 묶어서 한 건으로 보낸다. */
    @Value("${app.policy-change.max-per-user:3}")
    private int maxPerUser;

    private final PolicyChangeRepository changeRepository;
    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher dispatcher;
    private final EventLogger eventLogger;

    @Getter
    public static class NotifyResult {
        private int changesProcessed;
        private int notificationsSent;

        @Override
        public String toString() {
            return String.format("변경 %d건 처리, 알림 %d건 발송", changesProcessed, notificationsSent);
        }
    }

    @Transactional
    public NotifyResult notifyPendingChanges() {
        NotifyResult result = new NotifyResult();

        List<PolicyChange> pending = changeRepository.findUnnotified(PageRequest.of(0, batchSize));
        if (pending.isEmpty()) {
            return result;
        }

        for (PolicyChange change : pending) {
            try {
                result.notificationsSent += notify(change);
            } catch (Exception e) {
                log.warn("정책 변경 알림 실패 - changeId={}, 사유={}", change.getId(), e.getMessage());
            } finally {
                // 실패해도 표시해 둔다. 재시도로 같은 알림이 반복되는 편이 더 나쁘다.
                change.markNotified();
                result.changesProcessed++;
            }
        }

        log.info("정책 변경 알림 - {}", result);
        return result;
    }

    private int notify(PolicyChange change) {
        Policy policy = policyRepository.findById(change.getPolicyId()).orElse(null);
        if (policy == null || !Boolean.TRUE.equals(policy.getIsActive())) {
            return 0;
        }

        List<User> targets = findTargets(change);
        String title = buildTitle(change, policy);
        String message = buildMessage(change, policy);

        int sent = 0;
        for (User user : targets) {
            Notification notification = notificationRepository.save(Notification.builder()
                    .user(user)
                    .notificationType(Notification.NotificationType.POLICY)
                    .title(title)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build());

            dispatcher.dispatchAsync(notification);
            // 발송 대비 클릭률이 알림의 효과를 판단하는 유일한 지표다.
            eventLogger.log(EventType.NOTIFICATION_SENT, user.getId(),
                    String.valueOf(notification.getId()), change.getChangeType().name());
            sent++;
        }
        return sent;
    }

    /** 전국 정책은 모두에게, 지역 정책은 그 지역 주민에게만 알린다. */
    private List<User> findTargets(PolicyChange change) {
        String region = change.getTargetRegion();
        List<User> active = userRepository.findByIsActiveTrue();

        if (region == null || region.isBlank() || region.contains("전국")) {
            return active;
        }
        return active.stream()
                .filter(u -> u.getAddress() != null && !u.getAddress().isBlank())
                // 주소는 "충청북도 청주시 ...", 정책 지역은 "충청북도 청주시" 처럼 표기가 달라 양방향으로 본다.
                .filter(u -> u.getAddress().contains(region) || region.contains(u.getAddress()))
                .toList();
    }

    private String buildTitle(PolicyChange change, Policy policy) {
        return switch (change.getChangeType()) {
            case CREATED -> "새로운 지원금: " + policy.getTitle();
            case AMOUNT_CHANGED -> "지원금액 변경: " + policy.getTitle();
            case DEADLINE_CHANGED -> "신청기한 변경: " + policy.getTitle();
            case AGE_RANGE_CHANGED -> "대상 연령 변경: " + policy.getTitle();
        };
    }

    private String buildMessage(PolicyChange change, Policy policy) {
        if (change.getChangeType() == PolicyChange.ChangeType.CREATED) {
            String region = policy.getTargetRegion() == null ? "" : policy.getTargetRegion() + " ";
            return region + "지역에 새로운 지원금이 등록되었습니다. 대상 여부를 확인해 보세요.";
        }
        String from = change.getOldValue() == null ? "미상" : change.getOldValue();
        String to = change.getNewValue() == null ? "미상" : change.getNewValue();
        return String.format("%s이(가) %s → %s 로 변경되었습니다.",
                change.getChangeType().getDisplayName(), from, to);
    }
}
