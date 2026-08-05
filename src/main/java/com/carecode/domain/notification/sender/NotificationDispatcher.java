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
                .deviceToken(preference != null ? preference.getDeviceToken() : null)
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

    Optional<NotificationSender> senderFor(NotificationChannelType channel) {
        return Optional.ofNullable(senders.get(channel));
    }
}
