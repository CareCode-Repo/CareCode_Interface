package com.carecode.domain.notification.sender;

/**
 * 채널별 알림 발송 구현체.
 *
 * <p>구현체를 빈으로 등록하면 {@code NotificationDispatcher} 가 자동으로 수집한다.
 * 새 채널(카카오 알림톡 등)을 붙일 때는 이 인터페이스만 구현하면 된다.
 */
public interface NotificationSender {

    NotificationChannelType channel();

    /**
     * 발송을 시도한다.
     *
     * @return 발송 성공 여부. 실패 시 예외를 던지지 말고 false 를 반환한다
     *         (한 채널 실패가 다른 채널 발송을 막지 않도록).
     */
    boolean send(NotificationPayload payload);

    /**
     * 현재 설정으로 이 채널을 실제로 사용할 수 있는지.
     * 자격증명이 없으면 false 를 반환해 조용히 건너뛴다.
     */
    default boolean isAvailable() {
        return true;
    }
}
