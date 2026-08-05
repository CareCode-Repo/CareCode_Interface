package com.carecode.domain.notification.sender;

/** 채널별 알림 발송 구현체. 구현체를 빈으로 등록하면 NotificationDispatcher 가 자동으로 수집한다 */
public interface NotificationSender {

    NotificationChannelType channel();

    /** 발송을 시도한다. (한 채널 실패가 다른 채널 발송을 막지 않도록). */
    boolean send(NotificationPayload payload);

    /** 현재 설정으로 이 채널을 실제로 사용할 수 있는지. 자격증명이 없으면 false 를 반환해 조용히 건너뛴다. */
    default boolean isAvailable() {
        return true;
    }
}
