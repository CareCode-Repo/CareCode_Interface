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
import java.util.LinkedHashSet;
import com.carecode.domain.notification.repository.PushDeviceRepository;

/** 알림을 사용자 설정에 맞는 채널로 실제 발송한다. */
@Slf4j
@Component
public class NotificationDispatcher {

    private final Map<NotificationChannelType, NotificationSender> senders =
            new EnumMap<>(NotificationChannelType.class);
    private final NotificationPreferenceRepository preferenceRepository;
    private final PushDeviceRepository pushDeviceRepository;

    public NotificationDispatcher(List<NotificationSender> senderBeans,
                                  NotificationPreferenceRepository preferenceRepository,
                                  PushDeviceRepository pushDeviceRepository) {
        this.preferenceRepository = preferenceRepository;
        this.pushDeviceRepository = pushDeviceRepository;
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
        if (!canReceive(recipient)) {
            log.info("알림 발송 건너뜀 - 탈퇴·비활성 계정. notificationId={}, userId={}",
                    notification.getId(), recipient.getId());
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
                .deviceTokens(resolveDeviceTokens(recipient, preference))
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
     * 푸시를 보낼 기기 토큰 전부.
     *
     * <p>한 사람이 휴대폰과 웹을 함께 쓴다. 토큰을 하나만 고르면 나머지 기기는 아무 말 없이
     * 알림을 받지 못한다.
     *
     * <p>두 곳에서 모은다. {@code TBL_PUSH_DEVICE} 가 지금의 저장소이고, 알림 설정 행의
     * {@code deviceToken} 은 그 전에 쓰던 자리다. 예전에 등록해 둔 사용자가 앱을 다시 열어
     * 토큰을 새로 올리기 전까지는 옛 자리의 값으로 계속 받아야 하므로 함께 본다.
     * 같은 토큰이 양쪽에 있을 수 있어 중복은 걸러낸다.
     */
    private List<String> resolveDeviceTokens(User recipient, NotificationPreference preference) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>(pushDeviceRepository.findTokensByUser(recipient));

        if (preference != null && preference.getDeviceToken() != null
                && !preference.getDeviceToken().isBlank()) {
            tokens.add(preference.getDeviceToken());
        }
        tokens.addAll(preferenceRepository.findDeviceTokensByUser(recipient));

        return List.copyOf(tokens);
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
    /**
     * 받을 수 있는 계정인지. 탈퇴·비활성 계정에는 외부 발송을 하지 않는다.
     *
     * <p>탈퇴는 물리 삭제가 아니라서({@link User#withdraw()}) 행이 남고, <b>그 사용자가 남긴
     * 대기 등록·알림 설정·푸시 토큰도 함께 남는다.</b> 그래서 대기를 걸어 둔 사람이 탈퇴한 뒤
     * 그 시설에 자리가 나면, 빈자리 알림이 탈퇴한 계정의 기기로 푸시되고 익명화된 주소
     * ({@code deleted_<id>@deleted.local})로 메일이 나갔다.
     *
     * <p>발송 경로는 넷이지만(리마인더 배치, 빈자리·마감 알림, 사용자 API, 어드민) 전부 이
     * 디스패처를 거친다. 그래서 막는 자리도 여기 하나다 — 경로마다 검사를 넣으면 다음 경로가
     * 생길 때 또 빠진다.
     *
     * <p>관리자가 비활성화한 계정도 같게 본다. 로그인할 수 없는 계정에 알림을 보낼 이유가 없다.
     *
     * <p>인앱 알림 레코드 자체는 막지 않는다. 그건 이미 저장된 뒤라 여기서 되돌릴 수 없고,
     * 조회 API 가 인증을 요구하므로 탈퇴 계정은 어차피 볼 수 없다.
     */
    private boolean canReceive(User recipient) {
        return recipient.getDeletedAt() == null && Boolean.TRUE.equals(recipient.getIsActive());
    }

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
