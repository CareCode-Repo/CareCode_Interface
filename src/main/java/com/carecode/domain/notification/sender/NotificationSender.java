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

    /**
     * 사용할 수 없을 때 그 이유. 설정 화면에 그대로 보여줄 수 있는 문구다.
     *
     * 왜 못 쓰는지는 채널마다 다르고(자격증명 미설정, 사업자 미연동 등) 발송기 자신만 안다.
     * 사용 가능할 때는 {@code null}.
     */
    default String getUnavailableReason() {
        return null;
    }
}
