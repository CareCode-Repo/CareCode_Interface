package com.carecode.domain.notification.sender;

/** 알림 전달 채널. */
public enum NotificationChannelType {
    IN_APP("인앱", "inapp"),
    EMAIL("이메일", "email"),
    PUSH("푸시", "push"),
    SMS("SMS", "sms");

    private final String displayName;
    private final String key;

    NotificationChannelType(String displayName, String key) {
        this.displayName = displayName;
        this.key = key;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 외부에 노출하는 채널 이름.
     *
     * 채널별 설정 변경 API 가 받는 값(`.../channels/{channel}`)과 같아야 한다.
     * 클라이언트가 이 값을 그대로 되돌려 보내기 때문에 enum 이름(IN_APP)을 쓰지 않는다.
     */
    public String getKey() {
        return key;
    }
}
