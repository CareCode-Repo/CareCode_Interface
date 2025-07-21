package com.carecode.domain.notification.strategy;

import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.user.entity.User;

/**
 * 알림 전략 인터페이스
 * 각 알림 타입별로 다른 처리 로직을 구현할 수 있도록 함
 */
public interface NotificationStrategy {
    
    /**
     * 알림 타입 반환
     */
    String getNotificationType();
    
    /**
     * 알림 생성
     */
    Notification createNotification(User user, String title, String message);
    
    /**
     * 알림 처리 (전송, 저장 등)
     */
    void processNotification(Notification notification);
    
    /**
     * 알림 유효성 검사
     */
    boolean validateNotification(Notification notification);
    
    /**
     * 알림 우선순위 결정
     */
    String determinePriority(Notification notification);
} 