package com.carecode.domain.notification.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** SMS 채널 발송기. 아직 계약된 SMS 사업자가 없어 실제 전송은 하지 않는다. */
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
    public String getUnavailableReason() {
        return isAvailable() ? null : "문자 발송은 아직 준비 중이에요.";
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
