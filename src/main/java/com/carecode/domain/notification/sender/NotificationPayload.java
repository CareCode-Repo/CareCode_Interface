package com.carecode.domain.notification.sender;

import com.carecode.domain.user.entity.User;
import java.util.List;
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

    /**
     * 푸시 발송 대상 토큰들.
     *
     * <p>한 사람이 휴대폰과 웹을 함께 쓴다. 하나만 보내면 나머지는 아무 말 없이 알림을
     * 받지 못한다 — 사용자는 "알림이 안 와요" 라고만 말할 수 있고 어느 기기인지 모른다.
     */
    private final List<String> deviceTokens;

    /** SMS 수신 번호. */
    private final String phoneNumber;

    public String resolveEmailAddress() {
        if (emailAddress != null && !emailAddress.isBlank()) {
            return emailAddress;
        }
        return recipient != null ? recipient.getEmail() : null;
    }

    public List<String> resolveDeviceTokens() {
        return deviceTokens == null ? List.of() : deviceTokens;
    }

    public String resolvePhoneNumber() {
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            return phoneNumber;
        }
        return recipient != null ? recipient.getPhoneNumber() : null;
    }
}
