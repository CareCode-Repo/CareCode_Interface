package com.carecode.domain.notification.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** 이메일 채널 발송기. */
@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailNotificationSender(JavaMailSender mailSender,
                                   @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public NotificationChannelType channel() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public boolean isAvailable() {
        return fromAddress != null && !fromAddress.isBlank();
    }

    @Override
    public String getUnavailableReason() {
        return isAvailable() ? null : "이메일 발송이 아직 설정되지 않았어요.";
    }

    @Override
    public boolean send(NotificationPayload payload) {
        String to = payload.resolveEmailAddress();
        if (to == null || to.isBlank()) {
            log.warn("이메일 발송 건너뜀 - 수신 주소 없음");
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom(fromAddress);
            message.setSubject(payload.getTitle());
            message.setText(payload.getMessage());
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            // 한 채널의 실패가 다른 채널 발송을 막지 않도록 예외를 밖으로 던지지 않는다.
            log.error("이메일 발송 실패 - 수신자={}", to, e);
            return false;
        }
    }
}
