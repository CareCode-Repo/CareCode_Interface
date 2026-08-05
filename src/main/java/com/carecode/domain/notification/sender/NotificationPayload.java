package com.carecode.domain.notification.sender;

import com.carecode.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

/** 채널 구현체에 전달되는 발송 요청. */
@Getter
@Builder
public class NotificationPayload {

    private final User recipient;
    private final String title;
    private final String message;

    /** 이메일 수신 주소. 없으면 사용자 계정 이메일을 사용한다. */
    private final String emailAddress;

    /** 푸시 발송 대상 디바이스 토큰. */
    private final String deviceToken;

    /** SMS 수신 번호. */
    private final String phoneNumber;

    public String resolveEmailAddress() {
        if (emailAddress != null && !emailAddress.isBlank()) {
            return emailAddress;
        }
        return recipient != null ? recipient.getEmail() : null;
    }

    public String resolvePhoneNumber() {
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            return phoneNumber;
        }
        return recipient != null ? recipient.getPhoneNumber() : null;
    }
}
