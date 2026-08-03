package com.carecode.domain.notification.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SMS 채널 발송기.
 *
 * <p>아직 계약된 SMS 사업자가 없어 실제 전송은 하지 않는다.
 * 채널 배선은 완성해 두고, 사업자가 정해지면 {@link #send} 구현만 채우면 된다.
 * 설정이 없는 동안에는 {@link #isAvailable()} 이 false 라 디스패처가 건너뛴다.
 */
@Slf4j
@Component
public class SmsNotificationSender implements NotificationSender {

    private final boolean enabled;

    public SmsNotificationSender(@Value("${app.notification.sms.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public NotificationChannelType channel() {
        return NotificationChannelType.SMS;
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public boolean send(NotificationPayload payload) {
        String to = payload.resolvePhoneNumber();
        if (to == null || to.isBlank()) {
            return false;
        }
        // 사업자 연동 전까지는 발송을 시도하지 않고 실패로 보고한다.
        log.warn("SMS 발송 미구현 - 사업자 연동 필요 (수신번호 보유 여부: {})", true);
        return false;
    }
}
