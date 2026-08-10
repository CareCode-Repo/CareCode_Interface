package com.carecode.domain.notification.sender;

import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.entity.NotificationPreference;
import com.carecode.domain.notification.repository.NotificationPreferenceRepository;
import com.carecode.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 알림을 사용자 설정에 맞는 채널로 실제 발송한다. */
@Slf4j
@Component
public class NotificationDispatcher {

    private final Map<NotificationChannelType, NotificationSender> senders =
            new EnumMap<>(NotificationChannelType.class);
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationDispatcher(List<NotificationSender> senderBeans,
                                  NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
        for (NotificationSender sender : senderBeans) {
            senders.put(sender.channel(), sender);
        }
        log.info("알림 발송 채널 등록: {}", senders.keySet());
    }

    /** 저장된 알림을 사용자 설정 채널로 발송한다. */
    @Async("notificationExecutor")
    public void dispatchAsync(Notification notification) {
        dispatch(notification);
    }

    public boolean dispatch(Notification notification) {
        User recipient = notification.getUser();
        if (recipient == null) {
            log.warn("알림 발송 건너뜀 - 수신자 없음. notificationId={}", notification.getId());
            return false;
        }

        NotificationPreference preference = preferenceRepository
                .findByUserAndNotificationType(recipient, notification.getNotificationType())
                .orElse(null);

        NotificationPayload payload = NotificationPayload.builder()
                .recipient(recipient)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .emailAddress(preference != null ? preference.getEmailAddress() : null)
                .deviceToken(resolveDeviceToken(recipient, preference))
                .phoneNumber(preference != null ? preference.getPhoneNumber() : null)
                .build();

        boolean anySent = false;
        for (NotificationChannelType channel : NotificationChannelType.values()) {
            if (!isChannelEnabled(preference, channel)) {
                continue;
            }
            // 인앱은 알림 레코드 자체가 전달 수단이므로 별도 발송기가 없다.
            if (channel == NotificationChannelType.IN_APP) {
                anySent = true;
                continue;
            }

            NotificationSender sender = senders.get(channel);
            if (sender == null || !sender.isAvailable()) {
                continue;
            }

            boolean sent = sender.send(payload);
            anySent |= sent;
            log.debug("알림 발송 결과 - notificationId={}, channel={}, success={}",
                    notification.getId(), channel, sent);
        }

        return anySent;
    }

    /**
     * 푸시 대상 디바이스 토큰.
     *
     * 토큰 등록은 SYSTEM 설정 행에만 쓰는데 발송은 알림 유형별 행을 읽는다. 그래서 해당 유형의
     * 행만 보면 SYSTEM 알림 외에는 토큰이 없어 푸시가 조용히 실패한다. 토큰은 기기의 성질이므로
     * 유형과 무관하게 찾는다.
     */
    private String resolveDeviceToken(User recipient, NotificationPreference preference) {
        if (preference != null && preference.getDeviceToken() != null
                && !preference.getDeviceToken().isBlank()) {
            return preference.getDeviceToken();
        }

        return preferenceRepository.findDeviceTokensByUser(recipient).stream()
                .findFirst()
                .orElse(null);
    }

    /** 채널 사용 여부. 사용자 설정이 없으면 인앱과 푸시를 기본으로 켠다. */
    private boolean isChannelEnabled(NotificationPreference preference, NotificationChannelType channel) {
        if (preference == null) {
            return channel == NotificationChannelType.IN_APP || channel == NotificationChannelType.PUSH;
        }
        return switch (channel) {
            case IN_APP -> Boolean.TRUE.equals(preference.getInAppEnabled());
            case EMAIL -> Boolean.TRUE.equals(preference.getEmailEnabled());
            case PUSH -> Boolean.TRUE.equals(preference.getPushEnabled());
            case SMS -> Boolean.TRUE.equals(preference.getSmsEnabled());
        };
    }

    /**
     * 지금 설정으로 이 채널을 실제 발송할 수 있는지.
     *
     * 인앱은 알림 레코드 자체가 전달 수단이라 발송기가 없고 항상 가능하다.
     * {@link #dispatch} 가 채널을 건너뛸 때 보는 조건과 같아야 한다 — 다르면 설정 화면에서
     * 켤 수 있다고 안내한 채널이 실제로는 아무것도 보내지 않는다.
     */
    public boolean isChannelAvailable(NotificationChannelType channel) {
        if (channel == NotificationChannelType.IN_APP) {
            return true;
        }

        return senderFor(channel).filter(NotificationSender::isAvailable).isPresent();
    }

    /** 채널을 쓸 수 없는 이유. 쓸 수 있으면 빈 값. */
    public Optional<String> unavailableReason(NotificationChannelType channel) {
        if (isChannelAvailable(channel)) {
            return Optional.empty();
        }

        return senderFor(channel)
                .map(NotificationSender::getUnavailableReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .or(() -> Optional.of("지금은 이 방법으로 보낼 수 없어요."));
    }

    Optional<NotificationSender> senderFor(NotificationChannelType channel) {
        return Optional.ofNullable(senders.get(channel));
    }
}
