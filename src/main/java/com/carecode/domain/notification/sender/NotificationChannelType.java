package com.carecode.domain.notification.sender;

/** 알림 전달 채널. */
public enum NotificationChannelType {
    IN_APP("인앱"),
    EMAIL("이메일"),
    PUSH("푸시"),
    SMS("SMS");

    private final String displayName;

    NotificationChannelType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
